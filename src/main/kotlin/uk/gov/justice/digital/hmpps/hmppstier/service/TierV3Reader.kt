package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.domain.RecalculationSource.OnDemandRecalculation
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierSummaryEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierSummaryRepository
import uk.gov.justice.digital.hmpps.hmppstier.model.TierDetailsDto
import uk.gov.justice.digital.hmpps.hmppstier.model.TierDto
import java.util.*

@Service
class TierV3Reader(
    private val tierCalculationRepository: TierCalculationRepository,
    private val tierSummaryRepository: TierSummaryRepository,
    private val tierCalculationService: TierCalculationService,
    private val tierUpdater: TierUpdater,
) {
    fun getTierCounts() = tierSummaryRepository.getTierV3Counts().toMap().mapKeys { Tier.valueOf(it.key) }

    fun getLatestTierByCrn(crn: String): TierDto? = tierSummaryRepository.findByIdOrNull(crn)?.dto()
        ?: getLatestTierCalculation(crn)?.also { runCatching { tierUpdater.createSummary(it) } }?.dto()
        ?: tierCalculationService.calculateTierForCrn(crn, OnDemandRecalculation)?.dto()

    fun getLatestTierDetailsByCrn(crn: String): TierDetailsDto? = getLatestTierCalculation(crn)?.details()
        ?: tierCalculationService.calculateTierForCrn(crn, OnDemandRecalculation)?.details()

    fun getTierByCalculationId(crn: String, calculationId: UUID): TierDto? =
        tierCalculationRepository.findByCrnAndUuid(crn, calculationId)?.dto()

    fun getTierHistory(crn: String): List<TierDto> =
        tierCalculationRepository.findByCrnOrderByCreatedDesc(crn).mapNotNull { it.dto() }

    private fun getLatestTierCalculation(crn: String): TierCalculationEntity? =
        tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn)

    companion object {
        fun TierCalculationEntity.details() = data.tier?.name?.let { tier ->
            TierDetailsDto(
                tierScore = tier,
                calculationId = uuid,
                calculationDate = created,
                data = data,
            )
        }

        fun TierCalculationEntity.dto() = data.tier?.name?.let { tier ->
            TierDto(
                tierScore = tier,
                calculationId = uuid,
                calculationDate = created,
                changeReason = changeReason
            )
        }

        fun TierSummaryEntity.dto() = tier?.let { tier ->
            TierDto(
                tierScore = tier,
                calculationId = uuid,
                calculationDate = lastModified,
                changeReason = null
            )
        }
    }
}
