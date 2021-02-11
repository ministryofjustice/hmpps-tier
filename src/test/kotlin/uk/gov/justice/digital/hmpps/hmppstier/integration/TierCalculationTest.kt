package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.assertj.core.api.Assertions.assertThat
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
class TierCalculationTest : MockedEndpointsTestBase() {

  @Autowired
  lateinit var service: TierCalculationService

  @Test
  fun `calculate change and protect for SC custodial sentence`() {
    setupSCCustodialSentence()
    restOfSetup()

    val tier = service.calculateTierForCrn("123")
    assertThat(tier.data.change.tier).isEqualTo(ChangeLevel.ONE)
    assertThat(tier.data.protect.tier).isEqualTo(ProtectLevel.A)
  }

  @Test
  fun `calculate change and protect for NC custodial sentence`() {
    setupNCCustodialSentence()
    restOfSetup()

    val tier = service.calculateTierForCrn("123")
    assertThat(tier.data.change.tier).isEqualTo(ChangeLevel.ONE)
    assertThat(tier.data.protect.tier).isEqualTo(ProtectLevel.A)
  }

  @Test
  fun `do not calculate change for terminated custodial sentence`() {
    setupTerminatedCustodialSentence()
    restOfSetup()

    val tier = service.calculateTierForCrn("123")
    assertThat(tier.data.change.tier).isEqualTo(ChangeLevel.ZERO)
    assertThat(tier.data.protect.tier).isEqualTo(ProtectLevel.A)
  }

  @Test
  fun `calculate change for terminated non-custodial sentence with unpaid work and current non-custodial sentence`() {
    setupCurrentNonCustodialSentenceAndTerminatedNonCustodialSentenceWithUnpaidWork()
    setupNonRestrictiveRequirements()
    restOfSetup()

    val tier = service.calculateTierForCrn("123")
    assertThat(tier.data.change.tier).isEqualTo(ChangeLevel.ONE)
    assertThat(tier.data.protect.tier).isEqualTo(ProtectLevel.A)
  }

  @Test
  fun `calculate change and protect for non-custodial sentence with no restrictive requirements or unpaid work`() {
    setupNonCustodialSentenceWithNoUnpaidWork()
    setupNonRestrictiveRequirements()
    restOfSetup()

    val tier = service.calculateTierForCrn("123")
    assertThat(tier.data.change.tier).isEqualTo(ChangeLevel.ONE)
    assertThat(tier.data.protect.tier).isEqualTo(ProtectLevel.A)
  }

  @Test
  fun `do not calculate change for terminated non-custodial sentence with no restrictive requirements or unpaid work`() {
    setupTerminatedNonCustodialSentenceWithNoUnpaidWork()
    setupNonRestrictiveRequirements()
    restOfSetup()

    val tier = service.calculateTierForCrn("123")
    assertThat(tier.data.change.tier).isEqualTo(ChangeLevel.ZERO)
    assertThat(tier.data.protect.tier).isEqualTo(ProtectLevel.A)
  }

  @Test
  fun `do not calculate change for a non-custodial sentence with unpaid work`() {
    setupNonCustodialSentenceWithUnpaidWork()
    setupNonRestrictiveRequirements()
    restOfSetup()

    val tier = service.calculateTierForCrn("123")
    assertThat(tier.data.change.tier).isEqualTo(ChangeLevel.ZERO)
    assertThat(tier.data.protect.tier).isEqualTo(ProtectLevel.A)
  }

  @Test
  fun `do not calculate change for non-custodial sentence with restrictive requirements`() {
    setupNonCustodialSentenceWithNoUnpaidWork()
    setupRestrictiveRequirements()
    restOfSetup()

    val tier = service.calculateTierForCrn("123")
    assertThat(tier.data.change.tier).isEqualTo(ChangeLevel.ZERO)
    assertThat(tier.data.protect.tier).isEqualTo(ProtectLevel.A)
  }

  @Test
  fun `calculate change for concurrent custodial and non-custodial sentence with unpaid work`() {
    setupConcurrentCustodialAndNonCustodialSentenceWithUnpaidWork()
    setupRestrictiveRequirements()
    restOfSetup()

    val tier = service.calculateTierForCrn("123")
    assertThat(tier.data.change.tier).isEqualTo(ChangeLevel.ONE)
    assertThat(tier.data.protect.tier).isEqualTo(ProtectLevel.A)
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
      ).withBody(ApiResponses.nonCustodialConvictionResponse())
    )
  }

  private fun setupCurrentNonCustodialSentenceAndTerminatedNonCustodialSentenceWithUnpaidWork() {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/convictions")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.nonCustodialCurrentAndTerminatedConvictionWithUnpaidWorkResponse())
    )
  }

  private fun setupConcurrentCustodialAndNonCustodialSentenceWithUnpaidWork() {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/convictions")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.custodialAndNonCustodialUnpaid())
    )
  }

  private fun setupNonCustodialSentenceWithUnpaidWork() {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/convictions")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.nonCustodialUnpaidWorkConvictionResponse())
    )
  }

  private fun setupSCCustodialSentence() {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/convictions")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.custodialSCConvictionResponse())
    )
  }

  private fun setupNCCustodialSentence() {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/convictions")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.custodialNCConvictionResponse())
    )
  }

  private fun setupTerminatedCustodialSentence() {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/convictions")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.custodialTerminatedConvictionResponse())
    )
  }

  private fun setupTerminatedNonCustodialSentenceWithNoUnpaidWork() {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/convictions")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.nonCustodialTerminatedConvictionResponse())
    )
  }

  private fun setupRestrictiveRequirements() {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/convictions/\\d+/requirements")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.restrictiveRequirementsResponse())
    )
  }

  private fun setupNonRestrictiveRequirements() {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/123/convictions/\\d+/requirements")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.nonRestrictiveRequirementsResponse())
    )
  }
}
