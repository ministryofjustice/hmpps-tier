package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.domain.RecalculationSource.OnDemandRecalculation
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierSummaryEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierSummaryRepository
import uk.gov.justice.digital.hmpps.hmppstier.model.TierDto.Companion.getSuffix
import uk.gov.justice.digital.hmpps.hmppstier.model.TierV3DetailsDto
import uk.gov.justice.digital.hmpps.hmppstier.model.TierV3Dto
import java.util.*

@Service
class TierV3Reader(
    private val tierCalculationRepository: TierCalculationRepository,
    private val tierSummaryRepository: TierSummaryRepository,
    private val tierCalculationService: TierCalculationService,
    private val tierUpdater: TierUpdater,
) {
    fun getTierCounts() = tierSummaryRepository.getTierV3Counts().toMap().mapKeys { Tier.valueOf(it.key) }

    fun getLatestTierByCrn(crn: String): TierV3Dto? = tierSummaryRepository.findByIdOrNull(crn)?.dto()
        ?: getLatestTierCalculation(crn)?.also { runCatching { tierUpdater.createSummary(it) } }?.dto()
        ?: tierCalculationService.calculateTierForCrn(crn, OnDemandRecalculation)?.dto()

    fun getLatestTierDetailsByCrn(crn: String): TierV3DetailsDto? = getLatestTierCalculation(crn)?.details()
        ?: tierCalculationService.calculateTierForCrn(crn, OnDemandRecalculation)?.details()

    fun getTierByCalculationId(crn: String, calculationId: UUID): TierV3Dto? =
        tierCalculationRepository.findByCrnAndUuid(crn, calculationId)?.dto()

    fun getTierHistory(crn: String, changesOnly: Boolean): List<TierV3Dto> =
        tierCalculationRepository.findByCrnOrderByCreatedDesc(crn).map { it.dto() ?: it.dtoV2() }
            .let { if (changesOnly) it.distinctConsecutiveBy { Pair(it.tierScore, it.provisional) } else it }

    fun getLatestTierByCrns(crns: List<String>): Map<String, TierV3Dto?> {
        require(crns.size <= 20) { "A maximum of 20 CRNs can be requested at once, got ${crns.size}" }
        val summaries = tierSummaryRepository.findByCrnIn(crns)
        val found = summaries.associate { it.crn to it.dto() }
        return crns.associateWith { crn ->
            found[crn]
                ?: getLatestTierCalculation(crn)?.also { runCatching { tierUpdater.createSummary(it) } }?.dto()
                ?: tierCalculationService.calculateTierForCrn(crn, OnDemandRecalculation)?.dto()
        }
    }

    private fun getLatestTierCalculation(crn: String): TierCalculationEntity? =
        tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn)

    private fun <T, K> List<T>.distinctConsecutiveBy(selector: (T) -> K): List<T> = filterIndexed { index, tier ->
        index == 0 || this[index - 1].let { prev -> selector(tier) != selector(prev) }
    }

    companion object {
        fun TierCalculationEntity.details() = data.tier?.name?.let { tier ->
            TierV3DetailsDto(
                tierScore = tier,
                calculationId = uuid,
                calculationDate = created,
                data = data,
                provisional = data.provisional,
            )
        }

        fun TierCalculationEntity.dto() = data.tier?.name?.let { tier ->
            TierV3Dto(
                tierScore = tier,
                calculationId = uuid,
                calculationDate = created,
                changeReason = changeReason,
                provisional = data.provisional,
            )
        }

        fun TierCalculationEntity.dtoV2() = TierV3Dto(
            tierScore = protectLevel() + changeLevel() + getSuffix(data.deliusInputs?.registrations?.unsupervised),
            calculationId = uuid,
            calculationDate = created,
            changeReason = changeReason
        )

        fun TierSummaryEntity.dto() = tier?.let { tier ->
            TierV3Dto(
                tierScore = tier,
                calculationId = uuid,
                calculationDate = lastModified,
                changeReason = null,
                provisional = provisional,
            )
        }
    }
}
