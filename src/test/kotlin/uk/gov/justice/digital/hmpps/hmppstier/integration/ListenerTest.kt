package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService
import java.nio.file.Files
import java.nio.file.Paths

class ListenerTest : IntegrationTestBase() {

  @Autowired
  lateinit var listener: TierCalculationService

  lateinit var mockCommunityApiServer: ClientAndServer
  lateinit var mockCAssessmentApiServer: ClientAndServer

  @BeforeEach
  fun setupMockServer() {
    mockCommunityApiServer = ClientAndServer.startClientAndServer(8081)
    mockCAssessmentApiServer = ClientAndServer.startClientAndServer(8082)
  }

  @AfterEach
  fun tearDownServer() {
    mockCommunityApiServer.stop()
    mockCAssessmentApiServer.stop()
  }

  private val communityApiAssessmentsResponse: String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/assessments.json"))
  private val registrationsResponse: String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/registrations.json"))
  private val offenderResponse: String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/offender.json"))
  private val assessmentsApiAssessmentsResponse: String =
    Files.readString(Paths.get("src/test/resources/fixtures/assessment-api/assessments.json"))
  private val assessmentsApiNeedsResponse: String =
    Files.readString(Paths.get("src/test/resources/fixtures/assessment-api/needs.json"))

  @Test
  fun `calculate change and protect`() {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/assessments")).respond(
      response().withContentType(
        MediaType.APPLICATION_JSON
      ).withBody(communityApiAssessmentsResponse)
    )
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/registrations")).respond(
      response().withContentType(
        MediaType.APPLICATION_JSON
      ).withBody(registrationsResponse)
    )
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123")).respond(
      response().withContentType(
        MediaType.APPLICATION_JSON
      ).withBody(offenderResponse)
    )
    mockCAssessmentApiServer.`when`(request().withPath("/offenders/crn/123/assessments/latest"), Times.exactly(2)).respond(
      response().withContentType(
        MediaType.APPLICATION_JSON
      ).withBody(assessmentsApiAssessmentsResponse)
    )
    mockCAssessmentApiServer.`when`(request().withPath("/assessments/oasysSetId/1234/needs")).respond(
      response().withContentType(
        MediaType.APPLICATION_JSON
      ).withBody(assessmentsApiNeedsResponse)
    )

    val tier = listener.getTierByCrn("123")
    Assertions.assertThat(tier.changeLevel).isEqualTo(ChangeLevel.ONE)
    Assertions.assertThat(tier.protectLevel).isEqualTo(ProtectLevel.A)
  }
}
