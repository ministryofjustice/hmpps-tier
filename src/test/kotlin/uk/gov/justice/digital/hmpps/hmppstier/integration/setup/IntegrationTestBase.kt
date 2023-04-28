package uk.gov.justice.digital.hmpps.hmppstier.integration.setup

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times.exactly
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse
import org.mockserver.model.HttpResponse.notFoundResponse
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType.APPLICATION_JSON
import org.mockserver.model.Parameter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppstier.controller.SQSMessage
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import uk.gov.justice.digital.hmpps.hmppstier.service.TierChangeEvent
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month.JANUARY
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.util.UUID

@TestInstance(PER_CLASS)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  private val offenderEventsQueue by lazy { hmppsQueueService.findByQueueId("hmppsoffenderqueue") ?: throw MissingQueueException("HmppsQueue hmppsoffenderqueue not found") }
  private val offenderEventsDlqClient by lazy { offenderEventsQueue.sqsDlqClient as AmazonSQS }
  private val offenderEventsClient by lazy { offenderEventsQueue.sqsClient as AmazonSQSAsync }

  private val domainEventQueue by lazy { hmppsQueueService.findByQueueId("hmppsdomaineventsqueue") ?: throw MissingQueueException("HmppsQueue hmppsdomaineventsqueue not found") }
  private val domainEventQueueDlqClient by lazy { domainEventQueue.sqsDlqClient as AmazonSQS }
  private val domainEventQueueClient by lazy { domainEventQueue.sqsClient as AmazonSQSAsync }

  private val calculationCompleteQueue by lazy { hmppsQueueService.findByQueueId("hmppscalculationcompletequeue") ?: throw MissingQueueException("HmppsQueue hmppscalculationcompletequeue not found") }
  private val calculationCompleteClient by lazy { calculationCompleteQueue.sqsClient as AmazonSQSAsync }

  @Autowired
  internal lateinit var jwtHelper: JwtAuthHelper

  @Autowired
  lateinit var oauthMock: ClientAndServer

  @Autowired
  lateinit var communityApi: ClientAndServer

  @Autowired
  lateinit var tierToDeliusApi: ClientAndServer

  @Autowired
  lateinit var assessmentApi: ClientAndServer

  @Autowired
  private lateinit var tierCalculationRepository: TierCalculationRepository

  @BeforeEach
  fun `purge Queues`() {
    offenderEventsClient.purgeQueue(PurgeQueueRequest(offenderEventsQueue.queueUrl))
    calculationCompleteClient.purgeQueue(PurgeQueueRequest(calculationCompleteQueue.queueUrl))
    offenderEventsDlqClient.purgeQueue(PurgeQueueRequest(offenderEventsQueue.dlqUrl))
    domainEventQueueClient.purgeQueue(PurgeQueueRequest(domainEventQueue.queueUrl))
    domainEventQueueDlqClient.purgeQueue(PurgeQueueRequest(domainEventQueue.dlqUrl))
    tierCalculationRepository.deleteAll()
    communityApi.reset()
    assessmentApi.reset()
    tierToDeliusApi.reset()
    setupOauth()
  }

  private fun setupOauth() {
    val response = response().withContentType(APPLICATION_JSON)
      .withBody(objectMapper.writeValueAsString(mapOf("access_token" to "ABCDE", "token_type" to "bearer")))
    oauthMock.`when`(request().withPath("/auth/oauth/token").withBody("grant_type=client_credentials"))
      .respond(response)
  }

  internal fun HttpHeaders.authToken(roles: List<String> = emptyList()) {
    this.setBearerAuth(
      jwtHelper.createJwt(),
    )
  }

  fun setupNCCustodialSentence(crn: String) = setupActiveConvictions(crn, custodialNCConvictionResponse())

  fun setUpNoSentence(crn: String) = setupActiveConvictions(crn, noSentenceConvictionResponse())

  fun setupRegistrations(registrationsResponse: HttpResponse, crn: String) =
    communityApiResponseWithQs(
      registrationsResponse,
      "/secure/offenders/crn/$crn/registrations",
      Parameter("activeOnly", "true"),
    )

  fun setupEmptyNsisResponse(crn: String) {
    communityApiResponseWithQs(
      emptyNsisResponse(),
      "/secure/offenders/crn/$crn/convictions/2500222290/nsis",
      Parameter("nsiCodes", "BRE,BRES,REC,RECS"),
    )
  }

  fun restOfSetupWithMaleOffenderNoSevereNeeds(
    crn: String,
    includeAssessmentApi: Boolean = true,
    assessmentId: String,
    tier: String = "A1",
  ) {
    setupCommunityApiAssessment(crn)
    setupMaleOffender(crn, tier)
    if (includeAssessmentApi) {
      setupCurrentAssessment(crn, assessmentId)
    }
    setupNeeds(assessmentsApiNoSeverityNeedsResponse(), assessmentId)
  }

  fun setupCommunityApiAssessment(crn: String, rsr: String = "23.0", ogrs: String = "21") {
    communityApiResponse(communityApiAssessmentsResponse(rsr, ogrs), "/secure/offenders/crn/$crn/assessments")
  }

  fun setupMaleOffender(crn: String, tier: String = "A1") {
    communityApiResponse(maleOffenderResponse(tier), "/secure/offenders/crn/$crn/all")
    communityApiResponse(maleOffenderResponse(tier), "/secure/offenders/crn/$crn/all")
  }

  fun setupMaleOffenderNotFound(crn: String) = communityApiResponse(notFoundResponse(), "/secure/offenders/crn/$crn/all")

  fun restOfSetupWithFemaleOffender(crn: String, assessmentId: String) {
    setupNoDeliusAssessment(crn)
    communityApiResponse(femaleOffenderResponse(), "/secure/offenders/crn/$crn/all")
    setupCurrentAssessment(crn, assessmentId)
    setupNeeds(notFoundResponse(), assessmentId)
  }

  fun setupNoDeliusAssessment(crn: String) = communityApiResponse(emptyCommunityApiAssessmentsResponse(), "/secure/offenders/crn/$crn/assessments")

  fun setupNeeds(needs: HttpResponse, assessmentId: String) = assessmentApiResponse(needs, "/assessments/oasysSetId/$assessmentId/needs")

  fun setupCurrentAssessment(crn: String, assessmentId: String) = setupLatestAssessment(crn, LocalDate.now().year, assessmentId)

  fun setupOutdatedAssessment(crn: String, assessmentId: String) = setupLatestAssessment(crn, 2018, assessmentId)

  private fun setupLatestAssessment(crn: String, year: Int, assessmentId: String) =
    assessmentApiResponse(
      assessmentsApiAssessmentsResponse(
        LocalDateTime.of(year, JANUARY, 1, 0, 0)!!,
        assessmentId,
      ),
      "/offenders/crn/$crn/assessments/summary",
    )

  fun setupAssessmentNotFound(crn: String) =
    assessmentApiResponse(notFoundResponse(), "/offenders/crn/$crn/assessments/summary")

  fun setupNonCustodialSentence(crn: String) = setupActiveConvictions(crn, nonCustodialConvictionResponse())

  fun setupCurrentNonCustodialSentenceAndTerminatedNonCustodialSentence(crn: String) =
    setupActiveConvictions(crn, nonCustodialCurrentAndTerminatedConviction())

  fun setupConcurrentCustodialAndNonCustodialSentence(crn: String) =
    setupActiveConvictions(crn, custodialAndNonCustodialConvictions())

  fun setupTerminatedCustodialSentence(crn: String) =
    setupActiveConvictions(crn, custodialTerminatedConvictionResponse())

  fun setupTerminatedNonCustodialSentence(crn: String) =
    setupActiveConvictions(crn, nonCustodialTerminatedConvictionResponse())

  fun setupRestrictiveRequirements(crn: String) =
    setupRequirementsResponse(crn, restrictiveRequirementsResponse())

  fun setupAdditionalRequirements(crn: String) =
    setupRequirementsResponse(crn, additionalRequirementsResponse())

  fun setupNoRequirements(crn: String) =
    setupRequirementsResponse(crn, noRequirementsResponse())

  private fun setupRequirementsResponse(crn: String, response: HttpResponse): Array<out Any>? = communityApiResponseWithQs(
    response,
    "/secure/offenders/crn/$crn/convictions/\\d+/requirements",
    Parameter("activeOnly", "true"),
    Parameter("excludeSoftDeleted", "true"),
  )

  fun setupRestrictiveAndNonRestrictiveRequirements(crn: String) = setupRequirementsResponse(crn, restrictiveAndNonRestrictiveRequirementsResponse())

  fun setupNonRestrictiveRequirements(crn: String) = setupRequirementsResponse(crn, nonRestrictiveRequirementsResponse())

  fun setupMaleOffenderWithRegistrations(crn: String, includeAssessmentApi: Boolean = true, assessmentId: String, tier: String = "A1") {
    setupRegistrations(registrationsResponseWithMappa(), crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, includeAssessmentApi, assessmentId, tier)
  }

  fun setupSCCustodialSentence(crn: String) = setupActiveConvictions(crn, custodialSCConvictionResponse())

  private fun setupActiveConvictions(crn: String, response: HttpResponse) =
    communityApiResponseWithQs(response, "/secure/offenders/crn/$crn/convictions", Parameter("activeOnly", "true"))

  fun setupTierToDeliusFull(crn: String, ogrsscore: String = 21.toString(), rsrscore: String = 23.toString()) {
    tierToDeliusApiResponse(tierToDeliusFullResponse(ogrsscore = ogrsscore, rsrscore = rsrscore), "/tier-details/$crn")
  }

  fun setupTierToDeliusNoAssessment(crn: String, gender: String = "Male", currentTier: String = "UD0") {
    tierToDeliusApiResponse(tierToDeliusNoAssessmentResponse(gender = gender, currentTier = currentTier), "/tier-details/$crn")
  }

  fun setupTierToDeliusNoTierResponse(crn: String, gender: String = "Male") {
    tierToDeliusApiResponse(tierToDeliusNoTierResponse(gender = gender), "/tier-details/$crn")
  }

  fun calculateTierFor(crn: String) = putMessageOnQueue(offenderEventsClient, offenderEventsQueue.queueUrl, crn)
  fun calculateTierForDomainEvent(crn: String) = putMessageOnDomainQueue(domainEventQueueClient, domainEventQueue.queueUrl, crn)

  fun expectTierCalculationToHaveFailed() = oneMessageCurrentlyOnDeadletterQueue(offenderEventsDlqClient, offenderEventsQueue.dlqUrl!!)

  fun expectNoMessagesOnQueueOrDeadLetterQueue() {
    noMessagesCurrentlyOnQueue(offenderEventsClient, offenderEventsQueue.queueUrl)
    noMessagesCurrentlyOnDeadletterQueue(offenderEventsDlqClient, offenderEventsQueue.dlqUrl!!)
  }
  fun expectNoUpdatedTierCalculation() {
    // calculation succeeded but is unchanged, so no calculation complete events and message is not returned to the event queue
    noMessagesCurrentlyOnQueue(calculationCompleteClient, calculationCompleteQueue.queueUrl)
    noMessagesCurrentlyOnQueue(offenderEventsClient, offenderEventsQueue.queueUrl)
  }

  fun expectTierChangedById(tierScore: String) {
    oneMessageCurrentlyOnQueue(calculationCompleteClient, calculationCompleteQueue.queueUrl)
    val changeEvent: TierChangeEvent = tierChangeEvent()
    val crn = changeEvent.crn()
    val calculationId = changeEvent.calculationId()
    val detailUrl = "http://localhost:8080/crn/$crn/tier/$calculationId"
    assertThat(changeEvent.detailUrl).isEqualTo(detailUrl)
    assertThat(changeEvent.eventType).isEqualTo("tier.calculation.complete")
    assertThat(ZonedDateTime.parse(changeEvent.occurredAt, ISO_OFFSET_DATE_TIME)).isNotNull
    tierCalculationResult(crn, calculationId.toString())
      .isOk
      .expectBody()
      .jsonPath("tierScore").isEqualTo(tierScore)
  }

  fun expectTierCalculationNotFound(crn: String, calculationId: String) =
    tierCalculationResult(crn, calculationId)
      .isNotFound

  fun expectTierCalculationBadRequest(crn: String, calculationId: String) =
    tierCalculationResult(crn, calculationId)
      .isBadRequest

  private fun tierCalculationResult(crn: String, calculationId: String) = request("crn/$crn/tier/$calculationId")

  private fun request(uri: String) = webTestClient
    .get()
    .uri(uri)
    .headers(setAuthorisation())
    .exchange()
    .expectStatus()

  private fun latestTierCalculationResult(crn: String) = request("crn/$crn/tier")

  fun expectLatestTierCalculation(tierScore: String) {
    oneMessageCurrentlyOnQueue(calculationCompleteClient, calculationCompleteQueue.queueUrl)
    val crn: String = tierChangeEvent().crn()
    latestTierCalculationResult(crn)
      .isOk
      .expectBody()
      .jsonPath("tierScore").isEqualTo(tierScore)
  }

  fun expectLatestTierCalculationNotFound(crn: String) =
    latestTierCalculationResult(crn)
      .isNotFound

  private fun tierChangeEvent(): TierChangeEvent {
    val message = calculationCompleteClient.receiveMessage(calculationCompleteQueue.queueUrl)
    val sqsMessage: SQSMessage = objectMapper.readValue(message.messages[0].body, SQSMessage::class.java)
    return objectMapper.readValue(sqsMessage.message, TierChangeEvent::class.java)
  }

  fun TierChangeEvent.crn(): String = this.personReference.identifiers[0].value

  fun TierChangeEvent.calculationId(): UUID = this.additionalInformation.calculationId

  private fun httpSetup(response: HttpResponse, urlTemplate: String, clientAndServer: ClientAndServer) =
    clientAndServer.`when`(request().withPath(urlTemplate), exactly(1)).respond(response)

  private fun communityApiResponseWithQs(response: HttpResponse, urlTemplate: String, vararg qs: Parameter) =
    communityApi.`when`(request().withPath(urlTemplate).withQueryStringParameters(*qs), exactly(1)).respond(response)

  private fun communityApiResponse(response: HttpResponse, urlTemplate: String) =
    httpSetup(response, urlTemplate, communityApi)

  private fun assessmentApiResponse(response: HttpResponse, urlTemplate: String) =
    httpSetup(response, urlTemplate, assessmentApi)

  private fun tierToDeliusApiResponse(response: HttpResponse, urlTemplate: String) =
    httpSetup(response, urlTemplate, tierToDeliusApi)

  private fun setAuthorisation(): (HttpHeaders) -> Unit {
    val token = jwtHelper.createJwt()
    return { it.set(AUTHORIZATION, "Bearer $token") }
  }
}
