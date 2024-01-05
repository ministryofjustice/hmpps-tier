package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierSummary
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierSummaryRepository
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository

@Service
class TierUpdater(
    private val tierCalculationRepository: TierCalculationRepository,
    private val tierSummaryRepository: TierSummaryRepository,
) {

    fun removeTierCalculationsFor(crn: String) {
        tierCalculationRepository.deleteAllByCrn(crn)
        tierSummaryRepository.deleteById(crn)
    }

    @Transactional
    fun updateTier(
        tierCalculation: TierCalculationEntity,
        crn: String,
    ): Boolean {
        val isUpdated = isUpdated(tierCalculation, crn)
        tierCalculationRepository.save(tierCalculation)
        val summary = tierSummaryRepository.findByIdOrNull(tierCalculation.crn)?.apply {
            protectLevel = tierCalculation.protectLevel()
            changeLevel = tierCalculation.changeLevel()
        } ?: TierSummary(crn, tierCalculation.uuid, tierCalculation.protectLevel(), tierCalculation.changeLevel())
        tierSummaryRepository.save(summary)
        return isUpdated
    }

    private fun isUpdated(
        newTierCal: TierCalculationEntity,
        crn: String,
    ): Boolean {
        val oldTierCal = tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn)
        return newTierCal.data.protect.tier != oldTierCal?.data?.protect?.tier || newTierCal.data.change.tier != oldTierCal.data.change.tier
    }
}
