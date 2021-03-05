package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.notFoundResponse
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType.APPLICATION_JSON
import org.mockserver.model.RequestDefinition
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.assessmentsApiAssessmentsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.assessmentsApiNeedsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.communityApiAssessmentsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.custodialNCConvictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.emptyCommunityApiAssessmentsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.maleOffenderResponse
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate

abstract class MockedEndpointsTestBase : IntegrationTestBase() {
  lateinit var mockCommunityApiServer: ClientAndServer
  lateinit var mockAssessmentApiServer: ClientAndServer

  @BeforeAll
  fun setupMockServer() {
    mockCommunityApiServer = ClientAndServer.startClientAndServer(8081)
    mockAssessmentApiServer = ClientAndServer.startClientAndServer(8082)
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

  fun restOfSetupWithMaleOffender(crn: String, includeAssessmentApi: Boolean = true) {
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
      .respond(jsonResponseOf(assessmentsApiNeedsResponse()))
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

  fun tierUpdateWillSucceed(crn: String, score: String): RequestDefinition {
    val expectedTierUpdate = request().withPath("/secure/offenders/crn/$crn/tier/$score").withMethod("POST")

    mockCommunityApiServer.`when`(expectedTierUpdate).respond(jsonResponseOf("{}"))
    return expectedTierUpdate
  }

  fun jsonResponseOf(response: String) = response().withContentType(APPLICATION_JSON).withBody(response)
}
