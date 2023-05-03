package uk.gov.justice.digital.hmpps.hmppstier.controller

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.CommunityApiExtension.Companion.communityApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.tierToDeliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase

class DomainEventsListenerTest : IntegrationTestBase() {
  @Test
  fun `can calculate tier on domain event`() {
    val crn = "X432777"
    tierToDeliusApi.getFullDetails(crn)
    communityApi.getCustodialSCSentenceConviction(crn)
    setupMaleOffenderWithRegistrations(crn, assessmentId = 4234568890)

    calculateTierForDomainEvent(crn)
    expectLatestTierCalculation("A1")
  }
}
