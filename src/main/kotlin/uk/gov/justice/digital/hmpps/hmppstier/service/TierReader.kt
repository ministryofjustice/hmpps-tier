package uk.gov.justice.digital.hmpps.hmppstier.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.dto.TierDto
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import java.util.UUID

@Service
class TierReader(
  private val tierCalculationRepository: TierCalculationRepository,
) {
  fun getLatestTierByCrn(crn: String): TierDto? =
    getLatestTierCalculation(crn)?.let {
      log.info("Found latest tier calculation for $crn")
      TierDto.from(it)
    }

  fun getTierByCalculationId(crn: String, calculationId: UUID): TierDto? =
    tierCalculationRepository.findByCrnAndUuid(crn, calculationId)?.let {
      log.info("Found tier for $crn and $calculationId")
      TierDto.from(it)
    }

  suspend fun getCrns(): Flow<String> {
    return tierCalculationRepository.findDistinctCrn().asFlow()
  }

  private fun getLatestTierCalculation(crn: String): TierCalculationEntity? =
    tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn)

  companion object {
    private val log =
      LoggerFactory.getLogger(this::class.java)
  }
}
