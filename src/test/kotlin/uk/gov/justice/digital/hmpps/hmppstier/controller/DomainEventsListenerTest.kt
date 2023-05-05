package uk.gov.justice.digital.hmpps.hmppstier.controller

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.CommunityApiExtension.Companion.communityApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.domain.Sentence
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.tierToDeliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.TierDetails
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase

class DomainEventsListenerTest : IntegrationTestBase() {
  @Test
  fun `can calculate tier on domain event`() {
    val crn = "X432777"
    tierToDeliusApi.getFullDetails(crn, TierDetails(convictions = listOf(Conviction(sentence = Sentence(sentenceCode = "SC")))))
    setupMaleOffenderWithRegistrations(crn, assessmentId = 4234568890)

    calculateTierForDomainEvent(crn)
    expectLatestTierCalculation("A1")
  }
}
