package uk.gov.justice.digital.hmpps.hmppstier.integration.setup

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import com.google.gson.Gson
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse
import org.mockserver.model.HttpResponse.notFoundResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

abstract class MockedEndpointsTestBase : IntegrationTestBase() {

  @Autowired
  lateinit var gson: Gson

  @Autowired
  lateinit var calculationCompleteClient: AmazonSQSAsync

  @Value("\${calculation-complete.sqs-queue}")
  lateinit var calculationCompleteUrl: String

  @Autowired
  lateinit var offenderEventsAmazonSQSAsync: AmazonSQSAsync

  @Value("\${offender-events.sqs-queue}")
  lateinit var eventQueueUrl: String

  @Autowired
  internal lateinit var jwtHelper: JwtAuthHelper

  @BeforeEach
  fun `purge Queues`() {
    offenderEventsAmazonSQSAsync.purgeQueue(PurgeQueueRequest(eventQueueUrl))
    calculationCompleteClient.purgeQueue(PurgeQueueRequest(calculationCompleteUrl))
    oauthMockServer.resetAll()
    oauthMockServer.stubGrantToken()
  }
  companion object {
    internal val oauthMockServer = OAuthMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      oauthMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      oauthMockServer.stop()
    }
  }

  lateinit var communityApi: ClientAndServer
  lateinit var assessmentApi: ClientAndServer

  @BeforeAll
  fun setupMockServer() {
    communityApi = startClientAndServer(8091)
    assessmentApi = startClientAndServer(8092)
  }

  @AfterEach
  fun reset() {
    communityApi.reset()
    assessmentApi.reset()
  }

  @AfterAll
  fun tearDownServer() {
    communityApi.stop()
    assessmentApi.stop()
  }

  fun setupNCCustodialSentence(crn: String) {
    communityApi.`when`(
      request()
        .withPath("/secure/offenders/crn/$crn/convictions").withQueryStringParameter("activeOnly", "true")
    )
      .respond(custodialNCConvictionResponse())
  }

  fun setupRegistrations(registrationsResponse: HttpResponse, crn: String) =
    httpSetup(registrationsResponse, "/secure/offenders/crn/$crn/registrations", communityApi)

  private fun httpSetup(response: HttpResponse, urlTemplate: String, clientAndServer: ClientAndServer) =
    clientAndServer.`when`(request().withPath(urlTemplate)).respond(response)

  fun setupEmptyNsisResponse(crn: String) {
    communityApi.`when`(
      request().withPath("/secure/offenders/crn/$crn/convictions/2500222290/nsis")
        .withQueryStringParameter("nsiCodes", "BRE,BRES,REC,RECS")
    ).respond(
      emptyNsisResponse()
    )
  }

  fun restOfSetupWithMaleOffenderNoSevereNeeds(crn: String, includeAssessmentApi: Boolean = true) =
    restOfSetupWithNeeds(crn, includeAssessmentApi, assessmentsApiNoSeverityNeedsResponse())

  fun restOfSetupWithMaleOffenderAnd8PointNeeds(crn: String, includeAssessmentApi: Boolean = true) =
    restOfSetupWithNeeds(crn, includeAssessmentApi, assessmentsApi8NeedsResponse())

  fun restOfSetupWithMaleOffenderAndSevereNeeds(crn: String, includeAssessmentApi: Boolean = true) =
    restOfSetupWithNeeds(crn, includeAssessmentApi, assessmentsApiHighSeverityNeedsResponse())

  private fun restOfSetupWithNeeds(crn: String, includeAssessmentApi: Boolean, needs: HttpResponse) {
    httpSetup(communityApiAssessmentsResponse(), "/secure/offenders/crn/$crn/assessments", communityApi)
    httpSetup(maleOffenderResponse(), "/secure/offenders/crn/$crn", communityApi)

    if (includeAssessmentApi) {
      setupCurrentAssessment(crn)
    }
    httpSetup(needs, "/assessments/oasysSetId/1234/needs", assessmentApi)
  }

  fun restOfSetupWithFemaleOffender(crn: String) {
    httpSetup(emptyCommunityApiAssessmentsResponse(), "/secure/offenders/crn/$crn/assessments", communityApi)
    httpSetup(femaleOffenderResponse(), "/secure/offenders/crn/$crn", communityApi)
    setupCurrentAssessment(crn)
    httpSetup(notFoundResponse(), "/assessments/oasysSetId/1234/needs", assessmentApi)
  }

  fun setupCurrentAssessment(crn: String) = setupLatestAssessment(crn, LocalDate.now().year)

  fun setupLatestAssessment(crn: String, year: Int) =
    httpSetup(assessmentsApiAssessmentsResponse(year), "/offenders/crn/$crn/assessments/summary", assessmentApi)

  fun setupAssessmentNotFound(crn: String) =
    httpSetup(notFoundResponse(), "/offenders/crn/$crn/assessments/summary", assessmentApi)

  fun setupNonCustodialSentence(crn: String) {
    communityApi.`when`(
      request().withPath("/secure/offenders/crn/$crn/convictions").withQueryStringParameter("activeOnly", "true")
    ).respond(nonCustodialConvictionResponse())
  }

  fun setupCurrentNonCustodialSentenceAndTerminatedNonCustodialSentence(crn: String) {
    communityApi.`when`(
      request().withPath("/secure/offenders/crn/$crn/convictions").withQueryStringParameter("activeOnly", "true")
    ).respond(nonCustodialCurrentAndTerminatedConviction())
  }

  fun setupConcurrentCustodialAndNonCustodialSentence(crn: String) {
    communityApi.`when`(
      request().withPath("/secure/offenders/crn/$crn/convictions").withQueryStringParameter("activeOnly", "true")
    ).respond(custodialAndNonCustodialConvictions())
  }

  fun setupTerminatedCustodialSentence(crn: String) {
    communityApi.`when`(
      request().withPath("/secure/offenders/crn/$crn/convictions").withQueryStringParameter("activeOnly", "true")
    ).respond(custodialTerminatedConvictionResponse())
  }

  fun setupTerminatedNonCustodialSentence(crn: String) {
    communityApi.`when`(
      request().withPath("/secure/offenders/crn/$crn/convictions").withQueryStringParameter("activeOnly", "true")
    ).respond(nonCustodialTerminatedConvictionResponse())
  }

  fun setupRestrictiveRequirements(crn: String) =
    httpSetup(restrictiveRequirementsResponse(), "/secure/offenders/crn/$crn/convictions/\\d+/requirements", communityApi)

  fun setupUnpaidWorkRequirements(crn: String) {
    communityApi.`when`(request().withPath("/secure/offenders/crn/$crn/convictions/\\d+/requirements"))
      .respond(
        unpaidWorkRequirementsResponse()
      )
  }

  fun setupNoRequirements(crn: String) {
    communityApi.`when`(request().withPath("/secure/offenders/crn/$crn/convictions/\\d+/requirements"))
      .respond(
        noRequirementsResponse()
      )
  }

  fun setupRestrictiveAndNonRestrictiveRequirements(crn: String) {
    communityApi.`when`(request().withPath("/secure/offenders/crn/$crn/convictions/\\d+/requirements"))
      .respond(restrictiveAndNonRestrictiveRequirementsResponse())
  }

  fun setupNonRestrictiveRequirements(crn: String) {
    communityApi.`when`(request().withPath("/secure/offenders/crn/$crn/convictions/\\d+/requirements"))
      .respond(nonRestrictiveRequirementsResponse())
  }

  fun setupMaleOffenderWithRegistrations(crn: String, includeAssessmentApi: Boolean = true) {
    setupRegistrations(registrationsResponse(), crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, includeAssessmentApi)
  }

  fun setupSCCustodialSentence(crn: String) {
    communityApi.`when`(
      request().withPath("/secure/offenders/crn/$crn/convictions").withQueryStringParameter("activeOnly", "true")
    ).respond(custodialSCConvictionResponse())
  }

  fun calculateTierFor(crn: String) {
    putMessageOnQueue(offenderEventsAmazonSQSAsync, eventQueueUrl, crn)
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
      .headers(setAuthorisation("ROLE_HMPPS_TIER"))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("tierScore").isEqualTo(tierScore)
  }

  internal fun setAuthorisation(role: String): (HttpHeaders) -> Unit {
    val token = jwtHelper.createJwt(
      subject = "hmpps-tier",
      scope = listOf("read"),
      expiryTime = Duration.ofHours(1L),
      roles = listOf(role)
    )
    return { it.set(HttpHeaders.AUTHORIZATION, "Bearer $token") }
  }
}

data class TierChangeEvent(
  val crn: String,
  val calculationId: UUID
)

data class SQSMessage(
  val Message: String,
  val MessageId: String
)
