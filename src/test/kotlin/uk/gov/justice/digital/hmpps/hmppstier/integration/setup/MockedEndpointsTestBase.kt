package uk.gov.justice.digital.hmpps.hmppstier.integration.setup

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import com.google.gson.Gson
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse
import org.mockserver.model.HttpResponse.notFoundResponse
import org.mockserver.model.MediaType.APPLICATION_JSON
import org.mockserver.model.Parameter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppstier.service.TierChangeEvent
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

@TestInstance(Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class MockedEndpointsTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var gson: Gson

  @Autowired
  lateinit var calculationCompleteClient: AmazonSQSAsync

  @Value("\${calculation-complete.sqs-queue}")
  lateinit var calculationCompleteUrl: String

  @Autowired
  lateinit var offenderEventsClient: AmazonSQSAsync

  @Value("\${offender-events.sqs-queue}")
  lateinit var eventQueueUrl: String

  @Autowired
  internal lateinit var jwtHelper: JwtAuthHelper

  @Autowired
  lateinit var oauthMock: ClientAndServer
  @Autowired
  lateinit var communityApi: ClientAndServer
  @Autowired
  lateinit var assessmentApi: ClientAndServer

  @BeforeEach
  fun `purge Queues`() {
    offenderEventsClient.purgeQueue(PurgeQueueRequest(eventQueueUrl))
    calculationCompleteClient.purgeQueue(PurgeQueueRequest(calculationCompleteUrl))

    setupOauth()
  }

  private fun setupOauth() {
    val response = HttpResponse.response().withContentType(APPLICATION_JSON)
      .withBody(gson.toJson(mapOf("access_token" to "ABCDE", "token_type" to "bearer")))
    oauthMock.`when`(request().withPath("/auth/oauth/token").withBody("grant_type=client_credentials")).respond(response)
  }

  fun setupNCCustodialSentence(crn: String) = setupActiveConvictions(crn, custodialNCConvictionResponse())

  fun setUpNoSentence(crn: String) = setupActiveConvictions(crn, noSentenceConvictionResponse())

  fun setupRegistrations(registrationsResponse: HttpResponse, crn: String) =
    communityApiResponseWithQs(
      registrationsResponse,
      "/secure/offenders/crn/$crn/registrations",
      Parameter("activeOnly", "true")
    )

  fun setupEmptyNsisResponse(crn: String) =
    communityApiResponseWithQs(
      emptyNsisResponse(),
      "/secure/offenders/crn/$crn/convictions/2500222290/nsis",
      Parameter("nsiCodes", "BRE,BRES,REC,RECS")
    )

  fun restOfSetupWithMaleOffenderNoSevereNeeds(crn: String, includeAssessmentApi: Boolean = true, assessmentId: String) =
    restOfSetupWithNeeds(crn, includeAssessmentApi, assessmentsApiNoSeverityNeedsResponse(), assessmentId)

  fun restOfSetupWithMaleOffenderAndSevereNeeds(crn: String, includeAssessmentApi: Boolean = true, assessmentId: String) =
    restOfSetupWithNeeds(crn, includeAssessmentApi, assessmentsApiHighSeverityNeedsResponse(), assessmentId)

  private fun restOfSetupWithNeeds(crn: String, includeAssessmentApi: Boolean, needs: HttpResponse, assessmentId: String) {
    setupCommunityApiAssessment(crn)
    setupMaleOffender(crn)

    if (includeAssessmentApi) {
      setupCurrentAssessment(crn, assessmentId)
    }
    setupNeeds(needs, assessmentId)
  }

  fun setupCommunityApiAssessment(crn: String, rsr: BigDecimal = BigDecimal(23.0), ogrs: String = "21") {
    communityApiResponse(communityApiAssessmentsResponse(rsr, ogrs), "/secure/offenders/crn/$crn/assessments")
  }

  fun setupMaleOffender(crn: String) {
    communityApiResponse(maleOffenderResponse(), "/secure/offenders/crn/$crn")
  }

  fun restOfSetupWithFemaleOffender(crn: String, assessmentId: String) {
    setupNoDeliusAssessment(crn)
    communityApiResponse(femaleOffenderResponse(), "/secure/offenders/crn/$crn")
    setupCurrentAssessment(crn, assessmentId)
    setupNeeds(notFoundResponse(), assessmentId)
  }

  fun setupNoDeliusAssessment(crn: String) {
    communityApiResponse(emptyCommunityApiAssessmentsResponse(), "/secure/offenders/crn/$crn/assessments")
  }

  fun setupNeeds(needs: HttpResponse, assessmentId: String) {
    assessmentApiResponse(needs, "/assessments/oasysSetId/$assessmentId/needs")
  }

  fun setupCurrentAssessment(crn: String, assessmentId: String) = setupLatestAssessment(crn, LocalDate.now().year, assessmentId)

  fun setupLatestAssessment(crn: String, year: Int, assessmentId: String) =
    assessmentApiResponse(assessmentsApiAssessmentsResponse(LocalDateTime.of(year, Month.JANUARY, 1, 0, 0)!!, assessmentId), "/offenders/crn/$crn/assessments/summary")

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
    communityApiResponseWithQs(
      restrictiveRequirementsResponse(),
      "/secure/offenders/crn/$crn/convictions/\\d+/requirements",
      Parameter("activeOnly", "true")
    )

  fun setupAdditionalRequirements(crn: String) =
    communityApiResponseWithQs(
      additionalRequirementsResponse(),
      "/secure/offenders/crn/$crn/convictions/\\d+/requirements",
      Parameter("activeOnly", "true")
    )

  fun setupNoRequirements(crn: String) =
    communityApiResponseWithQs(
      noRequirementsResponse(),
      "/secure/offenders/crn/$crn/convictions/\\d+/requirements",
      Parameter("activeOnly", "true")
    )

  fun setupRestrictiveAndNonRestrictiveRequirements(crn: String) =
    communityApiResponseWithQs(
      restrictiveAndNonRestrictiveRequirementsResponse(),
      "/secure/offenders/crn/$crn/convictions/\\d+/requirements", Parameter("activeOnly", "true")
    )

  fun setupNonRestrictiveRequirements(crn: String) =
    communityApiResponseWithQs(
      nonRestrictiveRequirementsResponse(),
      "/secure/offenders/crn/$crn/convictions/\\d+/requirements", Parameter("activeOnly", "true")
    )

  fun setupMaleOffenderWithRegistrations(crn: String, includeAssessmentApi: Boolean = true, assessmentId: String) {
    setupRegistrations(registrationsResponseWithMappa(), crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, includeAssessmentApi, assessmentId)
  }

  fun setupSCCustodialSentence(crn: String) = setupActiveConvictions(crn, custodialSCConvictionResponse())

  private fun setupActiveConvictions(crn: String, response: HttpResponse) =
    communityApiResponseWithQs(response, "/secure/offenders/crn/$crn/convictions", Parameter("activeOnly", "true"))

  fun calculateTierFor(crn: String) = putMessageOnQueue(offenderEventsClient, eventQueueUrl, crn)

  fun expectTierCalculationToHaveFailed() {
    // the message goes back on the queue but is not visible until after the test ends
    oneMessageNotVisibleOnQueue(offenderEventsClient, eventQueueUrl)
  }

  fun expectNoUpdatedTierCalculation() {
    // calculation succeeded but is unchanged, so no calculation complete events
    // and message is not returned to the event queue
    noMessagesCurrentlyOnQueue(calculationCompleteClient, calculationCompleteUrl)
    noMessagesCurrentlyOnQueue(offenderEventsClient, eventQueueUrl)
  }

  fun expectTierCalculation(tierScore: String) {
    oneMessageCurrentlyOnQueue(calculationCompleteClient, calculationCompleteUrl)
    val message = calculationCompleteClient.receiveMessage(calculationCompleteUrl)
    val sqsMessage: SQSMessage = gson.fromJson(message.messages[0].body, SQSMessage::class.java)
    val changeEvent: TierChangeEvent = gson.fromJson(sqsMessage.Message, TierChangeEvent::class.java)
    webTestClient
      .get()
      .uri("crn/${changeEvent.crn}/tier/${changeEvent.calculationId}")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("tierScore").isEqualTo(tierScore)
  }

  private fun httpSetupWithQs(
    response: HttpResponse,
    urlTemplate: String,
    clientAndServer: ClientAndServer,
    qs: Parameter
  ) =
    clientAndServer.`when`(request().withPath(urlTemplate).withQueryStringParameter(qs)).respond(response)

  private fun httpSetup(response: HttpResponse, urlTemplate: String, clientAndServer: ClientAndServer) =
    clientAndServer.`when`(request().withPath(urlTemplate)).respond(response)

  private fun communityApiResponseWithQs(response: HttpResponse, urlTemplate: String, qs: Parameter) =
    httpSetupWithQs(response, urlTemplate, communityApi, qs)

  private fun communityApiResponse(response: HttpResponse, urlTemplate: String) =
    httpSetup(response, urlTemplate, communityApi)

  private fun assessmentApiResponse(response: HttpResponse, urlTemplate: String) =
    httpSetup(response, urlTemplate, assessmentApi)

  private fun setAuthorisation(): (HttpHeaders) -> Unit {
    val token = jwtHelper.createJwt(
      subject = "hmpps-tier",
      expiryTime = Duration.ofHours(1L)
    )
    return { it.set(AUTHORIZATION, "Bearer $token") }
  }
}

data class SQSMessage(
  val Message: String,
  val MessageId: String
)
