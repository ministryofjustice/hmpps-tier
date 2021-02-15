package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType.APPLICATION_JSON
import org.mockserver.model.RequestDefinition
import java.nio.file.Files
import java.nio.file.Paths

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
    val validMessage: String =
      Files.readString(Paths.get("src/test/resources/fixtures/sqs/tier-calculation-event.json")).replace("X373878", crn)

    return validMessage
  }

  fun setupNCCustodialSentence(crn: String) {
    mockCommunityApiServer.`when`(HttpRequest.request().withPath("/secure/offenders/crn/$crn/convictions")).respond(
      HttpResponse.response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.custodialNCConvictionResponse())
    )
  }

  fun setupRegistrations(registrationsResponse: String, crn: String) {
    mockCommunityApiServer.`when`(HttpRequest.request().withPath("/secure/offenders/crn/$crn/registrations")).respond(
      HttpResponse.response().withContentType(
        APPLICATION_JSON
      ).withBody(registrationsResponse)
    )
  }

  fun restOfSetup(crn: String) {
    mockCommunityApiServer.`when`(HttpRequest.request().withPath("/secure/offenders/crn/$crn/assessments")).respond(
      HttpResponse.response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.communityApiAssessmentsResponse())
    )

    mockCommunityApiServer.`when`(HttpRequest.request().withPath("/secure/offenders/crn/$crn")).respond(
      HttpResponse.response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.offenderResponse())
    )
    mockAssessmentApiServer.`when`(
      HttpRequest.request().withPath("/offenders/crn/$crn/assessments/latest"),
      Times.exactly(2)
    )
      .respond(
        HttpResponse.response().withContentType(
          APPLICATION_JSON
        ).withBody(ApiResponses.assessmentsApiAssessmentsResponse())
      )
    mockAssessmentApiServer.`when`(HttpRequest.request().withPath("/assessments/oasysSetId/1234/needs")).respond(
      HttpResponse.response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.assessmentsApiNeedsResponse())
    )
  }
  fun setupUpdateTierSuccess(crn: String, score: String): RequestDefinition {
    val expectedTierUpdate = HttpRequest.request().withPath("/offenders/crn/$crn/tier/$score").withMethod("POST")

    mockCommunityApiServer.`when`(expectedTierUpdate).respond(
      HttpResponse.response().withContentType(
        APPLICATION_JSON
      ).withBody("{}")
    )
    return expectedTierUpdate
  }
}
