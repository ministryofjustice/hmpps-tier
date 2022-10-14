package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository

@Service
class TierUpdater(
  private val tierCalculationRepository: TierCalculationRepository,
  private val communityApiService: CommunityApiService
) {

  @Transactional
  fun updateTier(
    it: TierCalculationEntity,
    crn: String
  ): Boolean {
    val isUpdated = isUpdated(it, crn)
    tierCalculationRepository.save(it)
    return isUpdated
  }

  private fun isUpdated(
    newTierCal: TierCalculationEntity,
    crn: String
  ): Boolean {
    val oldTierCal = tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn)
    return newTierCal.data.protect.tier != oldTierCal?.data?.protect?.tier || newTierCal.data.change.tier != oldTierCal.data.change.tier ||
      tierIsDifferentThanDelius(crn, newTierCal)
  }

  private fun tierIsDifferentThanDelius(crn: String, tier: TierCalculationEntity): Boolean {
    return communityApiService.getTier(crn) != tier.data.protect.tier.value.plus('_').plus(tier.data.change.tier.value)
  }
}
