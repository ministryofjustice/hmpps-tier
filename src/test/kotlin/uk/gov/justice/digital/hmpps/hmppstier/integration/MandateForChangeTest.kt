package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.CommunityApiExtension.Companion.communityApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.domain.Sentence
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.tierToDeliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.TierDetails
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import java.time.LocalDate

class MandateForChangeTest : IntegrationTestBase() {

  @Test
  fun `calculate change for concurrent custodial and non-custodial sentence`() {
    val crn = "X676767"
    tierToDeliusApi.getFullDetails(crn, TierDetails(convictions = listOf( Conviction(),
      Conviction(sentence = Sentence(sentenceCode = "SP")),)))
    communityApi.getRestrictiveRequirement(crn)
    setupMaleOffenderWithRegistrations(crn, assessmentId = 4234567892)
    calculateTierFor(crn)
    expectTierChangedById("A1")
  }

  @Test
  fun `do not calculate change for terminated custodial sentence`() {
    val crn = "X173878"
    tierToDeliusApi.getFullDetails(crn, TierDetails(convictions = listOf(Conviction(sentence = Sentence(terminationDate = LocalDate.now().minusDays(1))))))
    setupMaleOffenderWithRegistrations(crn, assessmentId = 4234567895)
    calculateTierFor(crn)
    expectTierChangedById("A0")
  }

  @Test
  fun `calculate change for terminated non-custodial sentence and current non-custodial sentence with non-restrictive requirements`() {
    val crn = "X505050"
    tierToDeliusApi.getFullDetails(crn, TierDetails(convictions = listOf(Conviction(id = 2500409583, sentence = Sentence(sentenceCode = "SP")),
      Conviction(id = 2500409584, active = false, sentence = Sentence(sentenceCode = "SP")))))
    communityApi.getNonRestrictiveRequirement(crn)
    setupMaleOffenderWithRegistrations(crn, assessmentId = 4234567896)
    calculateTierFor(crn)
    expectTierChangedById("A1")
  }

  @Test
  fun `do not calculate change for terminated non-custodial sentence with non-restrictive requirements`() {
    val crn = "X888888"
    tierToDeliusApi.getFullDetails(crn, TierDetails(convictions = listOf(Conviction(sentence = Sentence(terminationDate = LocalDate.now().minusDays(1))))))
    communityApi.getNonRestrictiveRequirement(crn)
    setupMaleOffenderWithRegistrations(crn, assessmentId = 4234567898)
    calculateTierFor(crn)
    expectTierChangedById("A0")
  }

  @Test
  fun `do not calculate change when only restrictive requirements are present on a non-custodial sentence`() {
    val crn = "X888866"
    tierToDeliusApi.getFullDetails(crn, TierDetails(convictions = listOf(Conviction(sentence = Sentence(sentenceCode = "SP")))))
    communityApi.getRestrictiveRequirement(crn)
    setupMaleOffenderWithRegistrations(crn, assessmentId = 4234567899)
    calculateTierFor(crn)
    expectTierChangedById("A0")
  }

  @Test
  fun `calculate change with restrictive and non-restrictive requirements on a non-custodial sentence`() {
    val crn = "X888855"
    tierToDeliusApi.getFullDetails(crn, TierDetails(convictions = listOf(Conviction(sentence = Sentence(sentenceCode = "SP")))))
    communityApi.getRestrictiveAndNonRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn, assessmentId = 4134567890)
    calculateTierFor(crn)
    expectTierChangedById("A1")
  }

  @Test
  fun `do not calculate change when no requirements are present on a non-custodial sentence`() {
    val crn = "X888844"
    tierToDeliusApi.getFullDetails(crn, TierDetails(convictions = listOf(Conviction(sentence = Sentence(sentenceCode = "SP")))))
    communityApi.getEmptyRequirements(crn)
    setupMaleOffenderWithRegistrations(crn, assessmentId = 4334567890)
    calculateTierFor(crn)
    expectTierChangedById("A0")
  }
}
