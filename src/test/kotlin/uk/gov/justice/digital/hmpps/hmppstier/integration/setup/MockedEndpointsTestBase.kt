package uk.gov.justice.digital.hmpps.hmppstier.integration.setup

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import com.google.gson.Gson
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse
import org.mockserver.model.HttpResponse.notFoundResponse
import org.mockserver.model.MediaType.APPLICATION_JSON
import org.mockserver.model.Parameter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import uk.gov.justice.digital.hmpps.hmppstier.service.TierChangeEvent
import java.time.Duration
import java.time.LocalDate

abstract class MockedEndpointsTestBase : IntegrationTestBase() {

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

  private var oauthMock: ClientAndServer = startClientAndServer(9090)
  private var communityApi: ClientAndServer = startClientAndServer(8091)
  private var assessmentApi: ClientAndServer = startClientAndServer(8092)

  @BeforeEach
  fun `purge Queues`() {
    offenderEventsClient.purgeQueue(PurgeQueueRequest(eventQueueUrl))
    calculationCompleteClient.purgeQueue(PurgeQueueRequest(calculationCompleteUrl))

    val response = HttpResponse.response().withContentType(APPLICATION_JSON)
      .withBody(gson.toJson(mapOf("access_token" to "ABCDE", "token_type" to "bearer")))
    httpSetup(response, "/auth/oauth/token", oauthMock)
  }

  @AfterEach
  fun reset() {
    communityApi.reset()
    assessmentApi.reset()
    oauthMock.reset()
  }

  @AfterAll
  fun tearDownServer() {
    communityApi.stop()
    assessmentApi.stop()
    oauthMock.stop()
  }

  fun setupNCCustodialSentence(crn: String) = setupActiveConvictions(crn, custodialNCConvictionResponse())

  fun setUpNoSentence(crn: String) = setupActiveConvictions(crn, noSentenceConvictionResponse())

  fun setupRegistrations(registrationsResponse: HttpResponse, crn: String) =
    communityApiResponse(registrationsResponse, "/secure/offenders/crn/$crn/registrations")

  fun setupEmptyNsisResponse(crn: String) =
    communityApiResponseWithQs(emptyNsisResponse(), "/secure/offenders/crn/$crn/convictions/2500222290/nsis", Parameter("nsiCodes", "BRE,BRES,REC,RECS"))

  fun restOfSetupWithMaleOffenderNoSevereNeeds(crn: String, includeAssessmentApi: Boolean = true) =
    restOfSetupWithNeeds(crn, includeAssessmentApi, assessmentsApiNoSeverityNeedsResponse())

  fun restOfSetupWithMaleOffenderAnd8PointNeeds(crn: String, includeAssessmentApi: Boolean = true) =
    restOfSetupWithNeeds(crn, includeAssessmentApi, assessmentsApi8NeedsResponse())

  fun restOfSetupWithMaleOffenderAndSevereNeeds(crn: String, includeAssessmentApi: Boolean = true) =
    restOfSetupWithNeeds(crn, includeAssessmentApi, assessmentsApiHighSeverityNeedsResponse())

  private fun restOfSetupWithNeeds(crn: String, includeAssessmentApi: Boolean, needs: HttpResponse) {
    communityApiResponse(communityApiAssessmentsResponse(), "/secure/offenders/crn/$crn/assessments")
    communityApiResponse(maleOffenderResponse(), "/secure/offenders/crn/$crn")

    if (includeAssessmentApi) {
      setupCurrentAssessment(crn)
    }
    assessmentApiResponse(needs, "/assessments/oasysSetId/1234/needs")
  }

  fun restOfSetupWithFemaleOffender(crn: String) {
    communityApiResponse(emptyCommunityApiAssessmentsResponse(), "/secure/offenders/crn/$crn/assessments")
    communityApiResponse(femaleOffenderResponse(), "/secure/offenders/crn/$crn")
    setupCurrentAssessment(crn)
    assessmentApiResponse(notFoundResponse(), "/assessments/oasysSetId/1234/needs")
  }

  fun setupCurrentAssessment(crn: String) = setupLatestAssessment(crn, LocalDate.now().year)

  fun setupLatestAssessment(crn: String, year: Int) =
    assessmentApiResponse(assessmentsApiAssessmentsResponse(year), "/offenders/crn/$crn/assessments/summary")

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
    communityApiResponse(restrictiveRequirementsResponse(), "/secure/offenders/crn/$crn/convictions/\\d+/requirements")

  fun setupUnpaidWorkRequirements(crn: String) =
    communityApiResponse(unpaidWorkRequirementsResponse(), "/secure/offenders/crn/$crn/convictions/\\d+/requirements")

  fun setupAdditionalRequirements(crn: String) =
    communityApiResponse(additionalRequirementsResponse(), "/secure/offenders/crn/$crn/convictions/\\d+/requirements")

  fun setupNoRequirements(crn: String) =
    communityApiResponse(noRequirementsResponse(), "/secure/offenders/crn/$crn/convictions/\\d+/requirements")

  fun setupRestrictiveAndNonRestrictiveRequirements(crn: String) =
    communityApiResponse(
      restrictiveAndNonRestrictiveRequirementsResponse(),
      "/secure/offenders/crn/$crn/convictions/\\d+/requirements"
    )

  fun setupNonRestrictiveRequirements(crn: String) =
    communityApiResponse(
      nonRestrictiveRequirementsResponse(),
      "/secure/offenders/crn/$crn/convictions/\\d+/requirements"
    )

  fun setupInactiveNonRestrictiveRequirements(crn: String) =
    communityApiResponse(
      inactiveNonRestrictiveRequirementsResponse(),
      "/secure/offenders/crn/$crn/convictions/\\d+/requirements"
    )

  fun setupMaleOffenderWithRegistrations(crn: String, includeAssessmentApi: Boolean = true) {
    setupRegistrations(registrationsResponse(), crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, includeAssessmentApi)
  }

  fun setupSCCustodialSentence(crn: String) = setupActiveConvictions(crn, custodialSCConvictionResponse())

  private fun setupActiveConvictions(crn: String, response: HttpResponse) =
    communityApiResponseWithQs(response, "/secure/offenders/crn/$crn/convictions", Parameter("activeOnly", "true"))

  fun calculateTierFor(crn: String) {
    putMessageOnQueue(offenderEventsClient, eventQueueUrl, crn)
  }

  fun expectNoTierCalculation() {
    // the message goes back on the queue but is not visible until after the test ends
    await untilCallTo {
      getNumberOfMessagesCurrentlyNotVisibleOnQueue(
        offenderEventsClient, eventQueueUrl
      )
    } matches { it == 1 }
  }

  fun expectTierCalculation(tierScore: String) {
    await untilCallTo {
      getNumberOfMessagesCurrentlyOnQueue(
        calculationCompleteClient,
        calculationCompleteUrl
      )
    } matches { it == 1 }
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

  private fun httpSetupWithQs(response: HttpResponse, urlTemplate: String, clientAndServer: ClientAndServer, qs: Parameter) =
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
      scope = listOf("read"),
      expiryTime = Duration.ofHours(1L),
      roles = listOf("ROLE_HMPPS_TIER")
    )
    return { it.set(AUTHORIZATION, "Bearer $token") }
  }
}

private data class SQSMessage(
  val Message: String,
  val MessageId: String
)
