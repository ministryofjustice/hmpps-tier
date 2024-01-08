package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.dto.TierDetailsDto
import uk.gov.justice.digital.hmpps.hmppstier.dto.TierDto
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
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
            log.info("Found latest tier calculation for $crn")
            TierDto.from(it)
        }

    fun getLatestTierDetailsByCrn(crn: String): TierDetailsDto? =
        getLatestTierCalculation(crn)?.let {
            log.info("Found latest tier calculation for $crn")
            TierDetailsDto.from(it)
        }

    fun getTierByCalculationId(crn: String, calculationId: UUID): TierDto? =
        tierCalculationRepository.findByCrnAndUuid(crn, calculationId)?.let {
            log.info("Found tier for $crn and $calculationId")
            TierDto.from(it)
        }

    private fun getLatestTierCalculation(crn: String): TierCalculationEntity? =
        tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn)

    companion object {
        private val log =
            LoggerFactory.getLogger(this::class.java)
    }
}
