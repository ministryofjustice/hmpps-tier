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

  @Test
  fun `calculate change and protect for custodial sentence`() {
    givenACustodialSentence()
    restOfSetup()

    val tier = service.calculateTierForCrn("123")
    Assertions.assertThat(tier.data.change.tier).isEqualTo(ChangeLevel.ONE)
    Assertions.assertThat(tier.data.protect.tier).isEqualTo(ProtectLevel.A)
  }

  @Test
  fun `calculate change and protect for non-custodial sentence with no restrictive requirements or unpaid work`() {
    setupNonCustodialSentenceWithNoUnpaidWork()
    givenNonRestrictiveRequirements()
    restOfSetup()

    val tier = service.calculateTierForCrn("123")
    Assertions.assertThat(tier.data.change.tier).isEqualTo(ChangeLevel.ONE)
    Assertions.assertThat(tier.data.protect.tier).isEqualTo(ProtectLevel.A)
  }

  @Test
  fun `do not calculate change for a non-custodial sentence with unpaid work`() {
    setupNonCustodialSentenceWithUnpaidWork()
    givenNonRestrictiveRequirements()
    restOfSetup()

    val tier = service.calculateTierForCrn("123")
    Assertions.assertThat(tier.data.change.tier).isEqualTo(ChangeLevel.ZERO)
    Assertions.assertThat(tier.data.protect.tier).isEqualTo(ProtectLevel.A)
  }

  @Test
  fun `do not calculate change for non-custodial sentence with restrictive requirements`() {
    setupNonCustodialSentenceWithNoUnpaidWork()
    setupRestrictiveRequirements()
    restOfSetup()

    val tier = service.calculateTierForCrn("123")
    Assertions.assertThat(tier.data.change.tier).isEqualTo(ChangeLevel.ZERO)
    Assertions.assertThat(tier.data.protect.tier).isEqualTo(ProtectLevel.A)
  }

  private fun restOfSetup() {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/assessments")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.communityApiAssessmentsResponse())
    )
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/registrations")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.registrationsResponse())
    )
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.offenderResponse())
    )
    mockAssessmentApiServer.`when`(request().withPath("/offenders/crn/123/assessments/latest"), Times.exactly(2))
      .respond(
        response().withContentType(
          APPLICATION_JSON
        ).withBody(ApiResponses.assessmentsApiAssessmentsResponse())
      )
    mockAssessmentApiServer.`when`(request().withPath("/assessments/oasysSetId/1234/needs")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.assessmentsApiNeedsResponse())
    )
  }

  private fun setupNonCustodialSentenceWithNoUnpaidWork() {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/convictions")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.nonCustodialNoUnpaidWorkConvictionResponse())
    )
  }
  private fun setupNonCustodialSentenceWithUnpaidWork() {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/convictions")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.nonCustodialConvictionResponse())
    )
  }

  private fun givenACustodialSentence() {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/convictions")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.custodialConvictionResponse())
    )
  }

  private fun setupRestrictiveRequirements() {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/convictions/\\d+/requirements")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.restrictiveRequirementsResponse())
    )
  }

  private fun givenNonRestrictiveRequirements() {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/convictions/\\d+/requirements")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.nonRestrictiveRequirementsResponse())
    )
  }
}
