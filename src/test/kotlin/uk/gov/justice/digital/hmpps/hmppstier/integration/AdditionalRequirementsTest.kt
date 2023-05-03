package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.CommunityApiExtension.Companion.communityApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.tierToDeliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase

class AdditionalRequirementsTest : IntegrationTestBase() {

  @Test
  fun `Additional requirements do not cause a processing error`() {
    val crn = "X833444"
    tierToDeliusApi.getFullDetails(crn)
    communityApi.getCommunitySentenceConviction(crn)
    communityApi.getMappaRegistration(crn, "M2")
    communityApi.getAdditionalRequirements(crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 4234567890)
    calculateTierFor(crn)
    expectTierChangedById("A0")
  }
}
