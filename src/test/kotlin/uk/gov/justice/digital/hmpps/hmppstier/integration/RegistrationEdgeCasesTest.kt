package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.AssessmentApiExtension
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.AssessmentApiExtension.Companion.assessmentApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.CommunityApiExtension.Companion.communityApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.tierToDeliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.assessmentsApiNoSeverityNeedsResponse

class RegistrationEdgeCasesTest : IntegrationTestBase() {

  @Test
  fun `calculate change and protect when no registrations are found`() {
    val crn = "X473878"
    tierToDeliusApi.getFullDetails(crn)
    communityApi.getCustodialNCSentenceConviction(crn)
    communityApi.getEmptyRegistration(crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 7234567890)
    calculateTierFor(crn)
    expectTierChangedById("B1")
  }

  @Test
  fun `calculate change and protect when registration level is missing`() {
    val crn = "X445509"
    tierToDeliusApi.getFullDetails(crn)
    communityApi.getCustodialNCSentenceConviction(crn)
    communityApi.getNoLevelRegistration(crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 6234567890)
    calculateTierFor(crn)
    expectTierChangedById("B1")
  }

  @Test
  fun `uses latest registration - two mappa registrations present`() {
    val crn = "X445599"
    tierToDeliusApi.getFullDetails(crn)
    communityApi.getCustodialNCSentenceConviction(crn)
    communityApi.getMultipleMappaRegistrations(crn)
    communityApi.getMaleOffenderResponse(crn)
    assessmentApi.getNoSeverityNeeds(6234507890)
    communityApi.getEmptyAssessmentResponse(crn)
    calculateTierFor(crn)
    expectTierChangedById("A2")
  }

  @Test
  fun `exclude historic registrations from tier calculation`() {
    val crn = "X445599"
    tierToDeliusApi.getNoAssessment(crn)
    communityApi.getCustodialNCSentenceConviction(crn)
    communityApi.getHistoricMappaRegistration(crn)
    communityApi.getMaleOffenderResponse(crn)
    assessmentApi.getNoSeverityNeeds(6234507890)
    communityApi.getEmptyAssessmentResponse(crn)
    calculateTierFor(crn)
    expectLatestTierCalculation("D2")
  }

  @Test
  fun `uses mappa registration when latest is non-mappa but has mappa registration level`() {
    val crn = "X445599"
    tierToDeliusApi.getFullDetails(crn)
    communityApi.getCustodialNCSentenceConviction(crn)
    communityApi.getMultipleMappaRegistrationsWithHistoricLatest(crn)
    communityApi.getMaleOffenderResponse(crn)
    assessmentApi.getNoSeverityNeeds(6234507890)
    communityApi.getEmptyAssessmentResponse(crn)
    calculateTierFor(crn)
    expectTierChangedById("B2")
  }
}
