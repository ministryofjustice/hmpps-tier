package uk.gov.justice.digital.hmpps.hmppstier.integration

import com.amazonaws.services.sqs.AmazonSQSAsync
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
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.assessmentsApiAssessmentsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.assessmentsApiHighSeverityNeedsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.assessmentsApiNoSeverityNeedsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.communityApiAssessmentsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.custodialAndNonCustodialUnpaid
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.custodialNCConvictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.custodialSCConvictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.custodialTerminatedConvictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.emptyCommunityApiAssessmentsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.maleOffenderResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.noRequirementsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.nonCustodialConvictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.nonCustodialCurrentAndTerminatedConvictionWithUnpaidWorkResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.nonCustodialTerminatedConvictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.nonCustodialUnpaidWorkConvictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.nonRestrictiveRequirementsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.restrictiveAndNonRestrictiveRequirementsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.restrictiveRequirementsResponse
import java.nio.file.Files
import java.nio.file.Paths
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

  init {
    SecurityContextHolder.getContext().authentication = TestingAuthenticationToken("user", "pw")
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
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

  fun calculationMessage(crn: String): String {
    return Files.readString(Paths.get("src/test/resources/fixtures/sqs/tier-calculation-event.json"))
      .replace("X373878", crn)
  }

  fun setupNCCustodialSentence(crn: String) {
    mockCommunityApiServer.`when`(
      request()
        .withPath("/secure/offenders/crn/$crn/convictions")
    )
      .respond(jsonResponseOf(custodialNCConvictionResponse()))
  }

  fun setupRegistrations(registrationsResponse: String, crn: String) {
    mockCommunityApiServer.`when`(
      request()
        .withPath("/secure/offenders/crn/$crn/registrations")
    )
      .respond(jsonResponseOf(registrationsResponse))
  }

  fun restOfSetupWithMaleOffenderNoSevereNeeds(crn: String, includeAssessmentApi: Boolean = true) {
    mockCommunityApiServer.`when`(
      request()
        .withPath("/secure/offenders/crn/$crn/assessments")
    )
      .respond(jsonResponseOf(communityApiAssessmentsResponse()))

    mockCommunityApiServer.`when`(
      request()
        .withPath("/secure/offenders/crn/$crn")
    )
      .respond(
        jsonResponseOf(maleOffenderResponse())
      )
    if (includeAssessmentApi) {
      setupLatestAssessment(crn, LocalDate.now().year)
    }
    mockAssessmentApiServer.`when`(
      request()
        .withPath("/assessments/oasysSetId/1234/needs")
    )
      .respond(jsonResponseOf(assessmentsApiNoSeverityNeedsResponse()))
  }

  fun restOfSetupWithMaleOffenderAndSevereNeeds(crn: String, includeAssessmentApi: Boolean = true) {
    mockCommunityApiServer.`when`(
      request()
        .withPath("/secure/offenders/crn/$crn/assessments")
    )
      .respond(jsonResponseOf(communityApiAssessmentsResponse()))

    mockCommunityApiServer.`when`(
      request()
        .withPath("/secure/offenders/crn/$crn")
    )
      .respond(
        jsonResponseOf(maleOffenderResponse())
      )
    if (includeAssessmentApi) {
      setupLatestAssessment(crn, LocalDate.now().year)
    }
    mockAssessmentApiServer.`when`(
      request()
        .withPath("/assessments/oasysSetId/1234/needs")
    )
      .respond(jsonResponseOf(assessmentsApiHighSeverityNeedsResponse()))
  }

  fun restOfSetupWithFemaleOffender(crn: String) {
    mockCommunityApiServer.`when`(
      request()
        .withPath("/secure/offenders/crn/$crn/assessments")
    )
      .respond(jsonResponseOf(emptyCommunityApiAssessmentsResponse()))

    mockCommunityApiServer.`when`(
      request()
        .withPath("/secure/offenders/crn/$crn")
    )
      .respond(jsonResponseOf(ApiResponses.femaleOffenderResponse()))
    setupLatestAssessment(crn, LocalDate.now().year)
    mockAssessmentApiServer.`when`(
      request()
        .withPath("/assessments/oasysSetId/1234/needs")
    )
      .respond(notFoundResponse())
  }

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

  fun setupNonCustodialSentenceWithNoUnpaidWork(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions")).respond(
      jsonResponseOf(nonCustodialConvictionResponse())
    )
  }

  fun setupCurrentNonCustodialSentenceAndTerminatedNonCustodialSentenceWithUnpaidWork(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions")).respond(
      jsonResponseOf(nonCustodialCurrentAndTerminatedConvictionWithUnpaidWorkResponse())
    )
  }

  fun setupConcurrentCustodialAndNonCustodialSentenceWithUnpaidWork(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions")).respond(
      jsonResponseOf(custodialAndNonCustodialUnpaid())
    )
  }

  fun setupNonCustodialSentenceWithUnpaidWork(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions")).respond(
      jsonResponseOf(nonCustodialUnpaidWorkConvictionResponse())
    )
  }

  fun setupTerminatedCustodialSentence(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions")).respond(
      jsonResponseOf(custodialTerminatedConvictionResponse())
    )
  }

  fun setupTerminatedNonCustodialSentenceWithNoUnpaidWork(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions")).respond(
      jsonResponseOf(nonCustodialTerminatedConvictionResponse())
    )
  }

  fun setupRestrictiveRequirements(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions/\\d+/requirements"))
      .respond(
        jsonResponseOf(restrictiveRequirementsResponse())
      )
  }

  fun setupNoRequirements(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions/\\d+/requirements"))
      .respond(
        jsonResponseOf(noRequirementsResponse())
      )
  }

  fun setupRestrictiveAndNonRestrictiveRequirements(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions/\\d+/requirements"))
      .respond(
        jsonResponseOf(restrictiveAndNonRestrictiveRequirementsResponse())
      )
  }

  fun setupNonRestrictiveRequirements(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions/\\d+/requirements"))
      .respond(
        jsonResponseOf(nonRestrictiveRequirementsResponse())
      )
  }

  fun setupMaleOffenderWithRegistrations(crn: String, includeAssessmentApi: Boolean = true) {
    setupRegistrations(ApiResponses.registrationsResponse(), crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, includeAssessmentApi)
  }

  fun setupSCCustodialSentence(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions")).respond(
      jsonResponseOf(custodialSCConvictionResponse())
    )
  }

  fun expectTierCalculation(tierScore: String) {
    await untilCallTo {
      getNumberOfMessagesCurrentlyOnQueue(
        calculationCompleteClient,
        calculationCompleteUrl
      )
    } matches { it == 1 }
    val message = calculationCompleteClient.receiveMessage(calculationCompleteUrl)
    val sqsMessage: SQSMessage = gson.fromJson(message.messages.get(0).body, SQSMessage::class.java)
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
