package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.dto.CalculationResultDto
import uk.gov.justice.digital.hmpps.hmppstier.dto.TierDto
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import java.time.Clock
import java.time.LocalDateTime

@Service
class TierCalculationService(
  private val clock: Clock,
  private val tierCalculationRepository: TierCalculationRepository,
  private val changeLevelCalculator: ChangeLevelCalculator,
  private val protectLevelCalculator: ProtectLevelCalculator,
  private val assessmentApiService: AssessmentApiService,
  private val communityApiClient: CommunityApiClient
) {

  fun getTierByCrn(crn: String): TierDto? =
    getLatestTierCalculation(crn)?.let {
      TierDto.from(it.data)
    }.also { log.info("Returned tier for $crn") }

  fun calculateTierForCrn(crn: String): CalculationResultDto {
    val existingTier = getLatestTierCalculation(crn)?.data

    return calculateTier(crn).let {
      CalculationResultDto(TierDto.from(it.data), it.data != existingTier)
    }
  }

  private fun calculateTier(crn: String): TierCalculationEntity {
    log.debug("Calculating tier for $crn")

    val offenderAssessment = assessmentApiService.getRecentAssessment(crn)

    val deliusAssessments = communityApiClient.getDeliusAssessments(crn)

    val deliusRegistrations = communityApiClient.getRegistrations(crn)

    val deliusConvictions = communityApiClient.getConvictions(crn)

    val protectLevel = protectLevelCalculator.calculateProtectLevel(
      crn,
      offenderAssessment,
      deliusAssessments,
      deliusRegistrations,
      deliusConvictions
    )
    val changeLevel = changeLevelCalculator.calculateChangeLevel(
      crn,
      offenderAssessment,
      deliusAssessments,
      deliusRegistrations,
      deliusConvictions
    )

    val calculation = TierCalculationEntity(
      crn = crn,
      created = LocalDateTime.now(clock),
      data = TierCalculationResultEntity(change = changeLevel, protect = protectLevel)
    )

    return tierCalculationRepository.save(calculation).also {
      log.info("Calculated tier for $crn ${calculation.data}")
    }
  }

  private fun getLatestTierCalculation(crn: String): TierCalculationEntity? {
    log.debug("Finding latest tier calculation for $crn")

    return tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn).also {
      when (it) {
        null -> log.info("No tier calculation found for $crn")
        else -> log.info("Found latest tier calculation for $crn")
      }
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(TierCalculationService::class.java)
  }
}
