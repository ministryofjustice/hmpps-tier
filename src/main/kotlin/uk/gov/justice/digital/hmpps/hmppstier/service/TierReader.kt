package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.dto.TierDetailsDto
import uk.gov.justice.digital.hmpps.hmppstier.dto.TierDto
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierSummary
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierSummaryRepository
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import uk.gov.justice.digital.hmpps.hmppstier.service.RecalculationSource.OnDemandRecalculation
import java.util.*

@Service
class TierReader(
    private val tierCalculationRepository: TierCalculationRepository,
    private val tierSummaryRepository: TierSummaryRepository,
    private val tierCalculationService: TierCalculationService,
    @Value("\${tier.unsupervised.suffix}") private val includeSuffix: Boolean
) {
    fun getTierCounts() = tierSummaryRepository.getTierCounts()

    fun getLatestTierByCrn(crn: String): TierDto? = tierSummaryRepository.findByIdOrNull(crn)
        ?.let { TierDto.from(it, includeSuffix) }
        ?: getLatestTierCalculation(crn)
            ?.let {
                try {
                    tierSummaryRepository.save(
                        TierSummary(
                            it.crn,
                            it.uuid,
                            it.protectLevel(),
                            it.changeLevel(),
                            it.data.deliusInputs?.registrations?.unsupervised == true,
                            0,
                            it.created
                        )
                    )
                } catch (ignored: Exception) {
                    // Doesn't matter if insert fails, should still return the result from the read
                }
                TierDto.from(it, includeSuffix)
            }
        ?: tierCalculationService.calculateTierForCrn(crn, OnDemandRecalculation, true)
            ?.let { TierDto.from(it, includeSuffix) }

    fun getLatestTierDetailsByCrn(crn: String): TierDetailsDto? =
        (getLatestTierCalculation(crn) ?: tierCalculationService.calculateTierForCrn(crn, OnDemandRecalculation, true))
            ?.let { TierDetailsDto.from(it, includeSuffix) }

    fun getTierByCalculationId(crn: String, calculationId: UUID): TierDto? =
        tierCalculationRepository.findByCrnAndUuid(crn, calculationId)
            ?.let { TierDto.from(it, includeSuffix) }

    fun getTierHistory(crn: String): List<TierDto> =
        tierCalculationRepository.findByCrnOrderByCreatedDesc(crn)
            .map { TierDto.from(it, includeSuffix) }

    private fun getLatestTierCalculation(crn: String): TierCalculationEntity? =
        tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn)
}
