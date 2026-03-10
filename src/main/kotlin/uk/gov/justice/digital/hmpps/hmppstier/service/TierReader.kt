package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.domain.RecalculationSource.OnDemandRecalculation
import uk.gov.justice.digital.hmpps.hmppstier.jpa.v1.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.v1.entity.TierSummaryEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.v1.repository.TierCalculationRepository
import uk.gov.justice.digital.hmpps.hmppstier.jpa.v1.repository.TierSummaryRepository
import uk.gov.justice.digital.hmpps.hmppstier.model.TierDetailsDto
import uk.gov.justice.digital.hmpps.hmppstier.model.TierDto
import java.util.*

@Service
class TierReader(
    private val tierCalculationRepository: TierCalculationRepository,
    private val tierSummaryRepository: TierSummaryRepository,
    private val tierCalculationService: TierCalculationService
) {
    fun getTierCounts() = tierSummaryRepository.getTierCounts()

    fun getLatestTierByCrn(crn: String): TierDto? = tierSummaryRepository.findByIdOrNull(crn)
        ?.let { TierDto.from(it) }
        ?: getLatestTierCalculation(crn)
            ?.let {
                try {
                    tierSummaryRepository.save(
                        TierSummaryEntity(
                            crn = it.crn,
                            uuid = it.uuid,
                            protectLevel = it.protectLevel(),
                            changeLevel = it.changeLevel(),
                            unsupervised = it.data.deliusInputs?.registrations?.unsupervised == true,
                            version = 0,
                            lastModified = it.created
                        )
                    )
                } catch (ignored: Exception) {
                    // Doesn't matter if insert fails, should still return the result from the read
                }
                TierDto.from(it)
            }
        ?: tierCalculationService.calculateTierForCrn(crn, OnDemandRecalculation)?.let { TierDto.from(it) }

    fun getLatestTierDetailsByCrn(crn: String): TierDetailsDto? =
        (getLatestTierCalculation(crn) ?: tierCalculationService.calculateTierForCrn(crn, OnDemandRecalculation))
            ?.let { TierDetailsDto.from(it) }

    fun getTierByCalculationId(crn: String, calculationId: UUID): TierDto? =
        tierCalculationRepository.findByCrnAndUuid(crn, calculationId)
            ?.let { TierDto.from(it) }

    fun getTierHistory(crn: String): List<TierDto> =
        tierCalculationRepository.findByCrnOrderByCreatedDesc(crn)
            .map { TierDto.from(it) }

    private fun getLatestTierCalculation(crn: String): TierCalculationEntity? =
        tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn)
}
