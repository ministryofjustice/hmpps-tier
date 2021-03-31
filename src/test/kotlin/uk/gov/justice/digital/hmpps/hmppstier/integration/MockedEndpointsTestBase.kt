package uk.gov.justice.digital.hmpps.hmppstier.integration

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
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse
import org.mockserver.model.HttpResponse.notFoundResponse
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType.APPLICATION_JSON
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

  @BeforeEach
  fun `purge Queues`() {
    offenderEventsAmazonSQSAsync.purgeQueue(PurgeQueueRequest(eventQueueUrl))
    calculationCompleteClient.purgeQueue(PurgeQueueRequest(calculationCompleteUrl))
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

  @BeforeEach
  fun resetStubs() {
    oauthMockServer.resetAll()
    oauthMockServer.stubGrantToken()
  }

  @Autowired
  internal lateinit var jwtHelper: JwtAuthHelper

  lateinit var mockCommunityApiServer: ClientAndServer
  lateinit var mockAssessmentApiServer: ClientAndServer

  @BeforeAll
  fun setupMockServer() {
    mockCommunityApiServer = ClientAndServer.startClientAndServer(8091)
    mockAssessmentApiServer = ClientAndServer.startClientAndServer(8092)
  }

  @AfterEach
  fun reset() {
    mockCommunityApiServer.reset()
    mockAssessmentApiServer.reset()
  }

  @AfterAll
  fun tearDownServer() {
    mockCommunityApiServer.stop()
    mockAssessmentApiServer.stop()
  }

  fun setupNCCustodialSentence(crn: String) {
    mockCommunityApiServer.`when`(
      request()
        .withPath("/secure/offenders/crn/$crn/convictions").withQueryStringParameter("activeOnly", "true")
    )
      .respond(custodialNCConvictionResponse())
  }

  fun setupRegistrations(registrationsResponse: HttpResponse, crn: String) {
    mockCommunityApiServer.`when`(
      request()
        .withPath("/secure/offenders/crn/$crn/registrations")
    )
      .respond(registrationsResponse)
  }

  fun restOfSetupWithMaleOffenderNoSevereNeeds(crn: String, includeAssessmentApi: Boolean = true) {
    restOfSetupWithNeeds(crn, includeAssessmentApi, assessmentsApiNoSeverityNeedsResponse())
  }

  fun restOfSetupWithMaleOffenderAnd8PointNeeds(crn: String, includeAssessmentApi: Boolean = true) {
    restOfSetupWithNeeds(crn, includeAssessmentApi, assessmentsApi8NeedsResponse())
  }

  fun restOfSetupWithMaleOffenderAndSevereNeeds(crn: String, includeAssessmentApi: Boolean = true) {
    restOfSetupWithNeeds(crn, includeAssessmentApi, assessmentsApiHighSeverityNeedsResponse())
  }

  fun emptyNsisResponse(crn: String) {
    mockCommunityApiServer
      .`when`(
        request()
          .withPath("/secure/offenders/crn/$crn/convictions/2500222290/nsis")
          .withQueryStringParameter("nsiCodes", "BRE,BRES,REC,RECS")
      )
      .respond(
        emptyNsiResponse()
      )
  }

  private fun restOfSetupWithNeeds(crn: String, includeAssessmentApi: Boolean, needs: String) {
    mockCommunityApiServer.`when`(
      request()
        .withPath("/secure/offenders/crn/$crn/assessments")
    )
      .respond(communityApiAssessmentsResponse())

    mockCommunityApiServer.`when`(
      request()
        .withPath("/secure/offenders/crn/$crn")
    )
      .respond(maleOffenderResponse())
    if (includeAssessmentApi) {
      setupCurrentAssessment(crn)
    }
    mockAssessmentApiServer.`when`(
      request()
        .withPath("/assessments/oasysSetId/1234/needs")
    )
      .respond(jsonResponseOf(needs))
  }

  fun restOfSetupWithFemaleOffender(crn: String) {
    mockCommunityApiServer.`when`(
      request()
        .withPath("/secure/offenders/crn/$crn/assessments")
    )
      .respond(emptyCommunityApiAssessmentsResponse())

    mockCommunityApiServer.`when`(
      request()
        .withPath("/secure/offenders/crn/$crn")
    )
      .respond(femaleOffenderResponse())
    setupCurrentAssessment(crn)
    mockAssessmentApiServer.`when`(
      request()
        .withPath("/assessments/oasysSetId/1234/needs")
    )
      .respond(notFoundResponse())
  }

  fun setupCurrentAssessment(crn: String) = setupLatestAssessment(crn, LocalDate.now().year)

  fun setupLatestAssessment(crn: String, year: Int) {
    mockAssessmentApiServer.`when`(
      request().withPath("/offenders/crn/$crn/assessments/summary"),
    )
      .respond(jsonResponseOf(assessmentsApiAssessmentsResponse(year)))
  }

  fun setupAssessmentNotFound(crn: String) {
    mockAssessmentApiServer.`when`(
      request().withPath("/offenders/crn/$crn/assessments/summary"),
    )
      .respond(notFoundResponse())
  }

  fun setupNonCustodialSentence(crn: String) {
    mockCommunityApiServer.`when`(
      request().withPath("/secure/offenders/crn/$crn/convictions").withQueryStringParameter("activeOnly", "true")
    ).respond(
      nonCustodialConvictionResponse()
    )
  }

  fun setupCurrentNonCustodialSentenceAndTerminatedNonCustodialSentence(crn: String) {
    mockCommunityApiServer.`when`(
      request().withPath("/secure/offenders/crn/$crn/convictions").withQueryStringParameter("activeOnly", "true")
    ).respond(
      jsonResponseOf(nonCustodialCurrentAndTerminatedConviction())
    )
  }

  fun setupConcurrentCustodialAndNonCustodialSentence(crn: String) {
    mockCommunityApiServer.`when`(
      request().withPath("/secure/offenders/crn/$crn/convictions").withQueryStringParameter("activeOnly", "true")
    ).respond(
      jsonResponseOf(custodialAndNonCustodialConvictions())
    )
  }

  fun setupTerminatedCustodialSentence(crn: String) {
    mockCommunityApiServer.`when`(
      request().withPath("/secure/offenders/crn/$crn/convictions").withQueryStringParameter("activeOnly", "true")
    ).respond(
      custodialTerminatedConvictionResponse()
    )
  }

  fun setupTerminatedNonCustodialSentence(crn: String) {
    mockCommunityApiServer.`when`(
      request().withPath("/secure/offenders/crn/$crn/convictions").withQueryStringParameter("activeOnly", "true")
    ).respond(
      nonCustodialTerminatedConvictionResponse()
    )
  }

  fun setupRestrictiveRequirements(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions/\\d+/requirements"))
      .respond(
        restrictiveRequirementsResponse()
      )
  }

  fun setupUnpaidWorkRequirements(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions/\\d+/requirements"))
      .respond(
        unpaidWorkRequirementsResponse()
      )
  }

  fun setupNoRequirements(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions/\\d+/requirements"))
      .respond(
        noRequirementsResponse()
      )
  }

  fun setupRestrictiveAndNonRestrictiveRequirements(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions/\\d+/requirements"))
      .respond(restrictiveAndNonRestrictiveRequirementsResponse())
  }

  fun setupNonRestrictiveRequirements(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions/\\d+/requirements"))
      .respond(nonRestrictiveRequirementsResponse())
  }

  fun setupMaleOffenderWithRegistrations(crn: String, includeAssessmentApi: Boolean = true) {
    setupRegistrations(registrationsResponse(), crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, includeAssessmentApi)
  }

  fun setupSCCustodialSentence(crn: String) {
    mockCommunityApiServer.`when`(
      request().withPath("/secure/offenders/crn/$crn/convictions").withQueryStringParameter("activeOnly", "true")
    ).respond(
      custodialSCConvictionResponse()
    )
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

  fun jsonResponseOf(response: String): HttpResponse = response().withContentType(APPLICATION_JSON).withBody(response)

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
