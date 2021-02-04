package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType.APPLICATION_JSON
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService
import java.nio.file.Files
import java.nio.file.Paths
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TierCalculationTest : IntegrationTestBase() {

  @Autowired
  lateinit var service: TierCalculationService

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

  private val communityApiAssessmentsResponse: String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/assessments.json"))
  private val registrationsResponse: String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/registrations.json"))
  private val custodialConvictionResponse: String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/convictions-custodial.json"))
  private val nonCustodialConvictionResponse: String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/convictions-non-custodial.json"))
  private val offenderResponse: String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/offender.json"))
  private val assessmentsApiAssessmentsResponse: String =
    Files.readString(Paths.get("src/test/resources/fixtures/assessment-api/assessments.json"))
  private val assessmentsApiNeedsResponse: String =
    Files.readString(Paths.get("src/test/resources/fixtures/assessment-api/needs.json"))

  @Test
  fun `calculate change and protect for custodial sentence`() {

    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/convictions")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(custodialConvictionResponse)
    )
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/assessments")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(communityApiAssessmentsResponse)
    )
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/registrations")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(registrationsResponse)
    )
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(offenderResponse)
    )
    mockAssessmentApiServer.`when`(request().withPath("/offenders/crn/123/assessments/latest"), Times.exactly(2)).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(assessmentsApiAssessmentsResponse)
    )
    mockAssessmentApiServer.`when`(request().withPath("/assessments/oasysSetId/1234/needs")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(assessmentsApiNeedsResponse)
    )

    val tier = service.calculateTierForCrn("123")
    Assertions.assertThat(tier.data.change.tier).isEqualTo(ChangeLevel.ONE)
    Assertions.assertThat(tier.data.protect.tier).isEqualTo(ProtectLevel.A)
  }

  @Test
  fun `change is zero for a non-custodial sentence with no restrictive requirements or unpaid work`() {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/convictions")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(nonCustodialConvictionResponse)
    )
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/assessments")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(communityApiAssessmentsResponse)
    )
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/registrations")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(registrationsResponse)
    )
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(offenderResponse)
    )
    mockAssessmentApiServer.`when`(request().withPath("/offenders/crn/123/assessments/latest"), Times.exactly(2)).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(assessmentsApiAssessmentsResponse)
    )
    mockAssessmentApiServer.`when`(request().withPath("/assessments/oasysSetId/1234/needs")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(assessmentsApiNeedsResponse)
    )

    val tier = service.calculateTierForCrn("123")
    Assertions.assertThat(tier.data.change.tier).isEqualTo(ChangeLevel.ZERO)
    Assertions.assertThat(tier.data.protect.tier).isEqualTo(ProtectLevel.A)
  }
}
