package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.dto.CalculationResultDto
import uk.gov.justice.digital.hmpps.hmppstier.dto.TierDto
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import java.time.Clock
import java.time.LocalDateTime

@Service
class TierCalculationService(
  private val tierCalculationRepository: TierCalculationRepository,
  private val clock: Clock,
  private val changeLevelCalculator: ChangeLevelCalculator,
  private val protectLevelCalculator: ProtectLevelCalculator
) {

  fun getOrCalculateTierByCrn(crn: String): TierDto {
    val result = getLatestTierCalculation(crn) ?: calculateTier(crn)
    return TierDto.from(result.data).also {
      log.info("Returned tier for $crn")
    }
  }

  fun calculateTierForCrn(crn: String): CalculationResultDto {
    val existingCalculation = getLatestTierCalculation(crn)
    val newTier = calculateTier(crn)
    val isUpdated: Boolean
    if (existingCalculation == null) {
      isUpdated = true
    } else {
      isUpdated = existingCalculation != newTier
    }

    return CalculationResultDto(TierDto.from(newTier.data), isUpdated)
  }

  private fun calculateTier(crn: String): TierCalculationEntity {
    log.debug("Calculating tier for $crn using 'New' calculation")

    val protectLevel = protectLevelCalculator.calculateProtectLevel(crn)
    val changeLevel = changeLevelCalculator.calculateChangeLevel(crn)

    val calculation = TierCalculationEntity(
      crn = crn,
      created = LocalDateTime.now(clock),
      data = TierCalculationResultEntity(change = changeLevel, protect = protectLevel)
    )

    return tierCalculationRepository.save(calculation).also {
      log.info("Calculated tier for $crn using 'New' calculation")
    }
  }

  private fun getLatestTierCalculation(crn: String): TierCalculationEntity? {
    log.debug("Finding latest tier calculation for $crn")

    return tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn).also {
      when (it) {
        null -> {
          log.info("No tier calculation found for $crn")
        }
        else -> {
          log.info("Found latest tier calculation for $crn")
        }
      }
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(TierCalculationService::class.java)
  }
}
