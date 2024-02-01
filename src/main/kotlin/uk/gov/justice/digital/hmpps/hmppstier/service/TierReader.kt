package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.dto.TierDetailsDto
import uk.gov.justice.digital.hmpps.hmppstier.dto.TierDto
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierSummary
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierSummaryRepository
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import java.util.*

@Service
class TierReader(
    private val tierCalculationRepository: TierCalculationRepository,
    private val tierSummaryRepository: TierSummaryRepository,
) {

    fun getTierCounts() = tierSummaryRepository.getTierCounts()

    fun getLatestTierByCrn(crn: String): TierDto? =
        tierSummaryRepository.findByIdOrNull(crn)?.let {
            TierDto.from(it)
        } ?: getLatestTierCalculation(crn)?.let {
            try {
                tierSummaryRepository.save(
                    TierSummary(
                        it.crn, it.uuid, it.protectLevel(), it.changeLevel(), 0, it.created
                    )
                )
            } catch (ignored: Exception) {
                // Doesn't matter if insert fails, should still return the result from the read
            }
            TierDto.from(it)
        }

    fun getLatestTierDetailsByCrn(crn: String): TierDetailsDto? =
        getLatestTierCalculation(crn)?.let {
            TierDetailsDto.from(it)
        }

    fun getTierByCalculationId(crn: String, calculationId: UUID): TierDto? =
        tierCalculationRepository.findByCrnAndUuid(crn, calculationId)?.let {
            TierDto.from(it)
        }

    private fun getLatestTierCalculation(crn: String): TierCalculationEntity? =
        tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn)
}
