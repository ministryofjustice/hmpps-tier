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
    log.info("Returned tier for $crn")
    return TierDto.from(result.data)
  }

  fun getTierCalculation(crn: String): CalculationResultDto {
    val latestTierCalculation = getLatestTierCalculation(crn) ?: return CalculationResultDto(null)
    return CalculationResultDto(TierDto.from(latestTierCalculation.data))
  }

  fun calculateTierForCrn(crn: String): TierDto = TierDto.from(calculateTier(crn).data)

  private fun calculateTier(crn: String): TierCalculationEntity {
    log.debug("Calculating tier for $crn using 'New' calculation")

    val protectLevel = protectLevelCalculator.calculateProtectLevel(crn)
    val changeLevel = changeLevelCalculator.calculateChangeLevel(crn)

    val calculation = TierCalculationEntity(
      crn = crn,
      created = LocalDateTime.now(clock),
      data = TierCalculationResultEntity(change = changeLevel, protect = protectLevel)
    )

    log.info("Calculated tier for $crn using 'New' calculation")
    return tierCalculationRepository.save(calculation)
  }

  private fun getLatestTierCalculation(crn: String): TierCalculationEntity? {
    log.debug("Finding latest tier calculation for $crn")

    val calculation = tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn)

    if (calculation == null) {
      log.info("No tier calculation found for $crn")
    } else {
      log.info("Found latest tier calculation for $crn")
    }
    return calculation
  }

  companion object {
    private val log = LoggerFactory.getLogger(TierCalculationService::class.java)
  }
}
