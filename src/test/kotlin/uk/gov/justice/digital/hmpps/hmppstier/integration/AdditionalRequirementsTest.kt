package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.CommunityApiExtension.Companion.communityApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.domain.Sentence
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.tierToDeliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.TierDetails
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase

class AdditionalRequirementsTest : IntegrationTestBase() {

  @Test
  fun `Additional requirements do not cause a processing error`() {
    val crn = "X833444"
    tierToDeliusApi.getFullDetails(crn, TierDetails(convictions = listOf(Conviction(sentence = Sentence(sentenceCode = "SP")))))
    communityApi.getMappaRegistration(crn, "M2")
    communityApi.getAdditionalRequirements(crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 4234567890)
    calculateTierFor(crn)
    expectTierChangedById("A0")
  }
}
