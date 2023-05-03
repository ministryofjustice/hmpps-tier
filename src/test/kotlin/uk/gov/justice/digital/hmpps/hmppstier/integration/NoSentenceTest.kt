package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.CommunityApiExtension
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase

class NoSentenceTest : IntegrationTestBase() {

  @Test
  fun `Tier is calculated with change level zero when no sentence is found`() {
    val crn = "X333444"
    setupTierToDeliusFull(crn)
    CommunityApiExtension.communityApi.getNoSentenceConviction(crn)
    CommunityApiExtension.communityApi.getMappaRegistration(crn, "M2")
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 5234567890)
    calculateTierFor(crn)
    expectTierChangedById("A0")
  }
}
