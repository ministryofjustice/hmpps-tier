package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.AssessmentApiExtension.Companion.assessmentApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.CommunityApiExtension.Companion.communityApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.tierToDeliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase

class NoAssessmentFoundTest : IntegrationTestBase() {

  @Test
  fun `changeLevel should be 2 if assessment returns 404`() {
    val crn = "X273878"
    tierToDeliusApi.getFullDetails(crn)
    communityApi.getCustodialNCSentenceConviction(crn)
    assessmentApi.getNotFoundAssessment(crn)
    communityApi.getMappaRegistration(crn, "M2")
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 8234567890)
    calculateTierFor(crn)
    expectTierChangedById("A2")
  }
}
