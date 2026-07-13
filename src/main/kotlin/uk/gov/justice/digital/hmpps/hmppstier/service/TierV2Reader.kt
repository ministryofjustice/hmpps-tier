package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.domain.RecalculationSource.OnDemandRecalculation
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierSummaryEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierSummaryRepository
import uk.gov.justice.digital.hmpps.hmppstier.model.TierDto.Companion.getSuffix
import uk.gov.justice.digital.hmpps.hmppstier.model.TierV2DetailsDto
import uk.gov.justice.digital.hmpps.hmppstier.model.TierV2Dto
import java.util.*

@Service
class TierV2Reader(
    private val tierCalculationRepository: TierCalculationRepository,
    private val tierSummaryRepository: TierSummaryRepository,
    private val tierCalculationService: TierCalculationService,
    private val tierUpdater: TierUpdater,
) {
    fun getTierCounts() = tierSummaryRepository.getTierV2Counts()

    fun getLatestTierByCrn(crn: String): TierV2Dto? = tierSummaryRepository.findByIdOrNull(crn)?.dto()
        ?: getLatestTierCalculation(crn)?.also { runCatching { tierUpdater.createSummary(it) } }?.dto()
        ?: tierCalculationService.calculateTierForCrn(crn, OnDemandRecalculation)?.dto()

    fun getLatestTierByCrns(crns: List<String>): Map<String, TierV2Dto?> {
        require(crns.size <= 20)
        val summaries = tierSummaryRepository.findByCrnIn(crns)
        val found = summaries.associate { it.crn to it.dto() }
        return crns.associateWith { crn ->
            found[crn]
                ?: getLatestTierCalculation(crn)?.also { runCatching { tierUpdater.createSummary(it) } }?.dto()
                ?: tierCalculationService.calculateTierForCrn(crn, OnDemandRecalculation)?.dto()
        }
    }

    fun getLatestTierDetailsByCrn(crn: String): TierV2DetailsDto? = getLatestTierCalculation(crn)?.details()
        ?: tierCalculationService.calculateTierForCrn(crn, OnDemandRecalculation)?.details()

    fun getTierByCalculationId(crn: String, calculationId: UUID): TierV2Dto? =
        tierCalculationRepository.findByCrnAndUuid(crn, calculationId)?.dto()

    fun getTierHistory(crn: String): List<TierV2Dto> =
        tierCalculationRepository.findByCrnOrderByCreatedDesc(crn).map { it.dto() }

    private fun getLatestTierCalculation(crn: String): TierCalculationEntity? =
        tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn)

    companion object {
        fun TierCalculationEntity.details() = TierV2DetailsDto(
            tierScore = data.protect.tier.value
                + data.change.tier.value
                + getSuffix(data.deliusInputs?.registrations?.unsupervised),
            calculationId = uuid,
            calculationDate = created,
            data = data,
        )

        fun TierCalculationEntity.dto() = TierV2Dto(
            tierScore = protectLevel() + changeLevel() + getSuffix(data.deliusInputs?.registrations?.unsupervised),
            calculationId = uuid,
            calculationDate = created,
            changeReason = changeReason
        )

        fun TierSummaryEntity.dto() = TierV2Dto(
            tierScore = protectLevel + changeLevel + getSuffix(unsupervised),
            calculationId = uuid,
            calculationDate = lastModified,
            changeReason = null
        )
    }
}
