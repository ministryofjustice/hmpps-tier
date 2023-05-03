package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.CommunityApiExtension
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.CommunityApiExtension.Companion.communityApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase

class MandateForChangeTest : IntegrationTestBase() {

  @Test
  fun `calculate change for concurrent custodial and non-custodial sentence`() {
    val crn = "X676767"
    setupTierToDeliusFull(crn)
    communityApi.getOneActiveCustodialAndOneActiveCommunityConviction(crn)
    communityApi.getRestrictiveRequirement(crn)
    setupMaleOffenderWithRegistrations(crn, assessmentId = "4234567892")
    calculateTierFor(crn)
    expectTierChangedById("A1")
  }

  @Test
  fun `do not calculate change for terminated custodial sentence`() {
    val crn = "X173878"
    setupTierToDeliusFull(crn)
    communityApi.getOneInactiveCustodialConviction(crn)
    setupMaleOffenderWithRegistrations(crn, assessmentId = "4234567895")
    calculateTierFor(crn)
    expectTierChangedById("A0")
  }

  @Test
  fun `calculate change for terminated non-custodial sentence and current non-custodial sentence with non-restrictive requirements`() {
    val crn = "X505050"
    setupTierToDeliusFull(crn)
    communityApi.getOneActiveAndOneInactiveCommunityConviction(crn)
     communityApi.getNonRestrictiveRequirement(crn)
    setupMaleOffenderWithRegistrations(crn, assessmentId = "4234567896")
    calculateTierFor(crn)
    expectTierChangedById("A1")
  }

  @Test
  fun `do not calculate change for terminated non-custodial sentence with non-restrictive requirements`() {
    val crn = "X888888"
    setupTierToDeliusFull(crn)
    communityApi.getOneInactiveCustodialConviction(crn)
     communityApi.getNonRestrictiveRequirement(crn)
    setupMaleOffenderWithRegistrations(crn, assessmentId = "4234567898")
    calculateTierFor(crn)
    expectTierChangedById("A0")
  }

  @Test
  fun `do not calculate change when only restrictive requirements are present on a non-custodial sentence`() {
    val crn = "X888866"
    setupTierToDeliusFull(crn)
    communityApi.getCommunitySentenceConviction(crn)
    communityApi.getRestrictiveRequirement(crn)
    setupMaleOffenderWithRegistrations(crn, assessmentId = "4234567899")
    calculateTierFor(crn)
    expectTierChangedById("A0")
  }

  @Test
  fun `calculate change with restrictive and non-restrictive requirements on a non-custodial sentence`() {
    val crn = "X888855"
    setupTierToDeliusFull(crn)
    communityApi.getCommunitySentenceConviction(crn)
    communityApi.getRestrictiveAndNonRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn, assessmentId = "4134567890")
    calculateTierFor(crn)
    expectTierChangedById("A1")
  }

  @Test
  fun `do not calculate change when no requirements are present on a non-custodial sentence`() {
    val crn = "X888844"
    setupTierToDeliusFull(crn)
    communityApi.getCommunitySentenceConviction(crn)
    communityApi.getEmptyRequirements(crn)
    setupMaleOffenderWithRegistrations(crn, assessmentId = "4334567890")
    calculateTierFor(crn)
    expectTierChangedById("A0")
  }
}
