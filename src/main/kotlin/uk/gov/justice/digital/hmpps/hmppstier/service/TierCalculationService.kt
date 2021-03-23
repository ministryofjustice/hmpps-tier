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
import java.util.UUID

@Service
class TierCalculationService(
  private val clock: Clock,
  private val tierCalculationRepository: TierCalculationRepository,
  private val changeLevelCalculator: ChangeLevelCalculator,
  private val protectLevelCalculator: ProtectLevelCalculator,
  private val assessmentApiService: AssessmentApiService,
  private val communityApiClient: CommunityApiClient,
  private val telemetryService: TelemetryService
) {

  fun getLatestTierByCrn(crn: String): TierDto? =
    getLatestTierCalculation(crn)?.let {
      TierDto.from(it)
    }.also { log.info("Returned tier for $crn") }

  fun getTierByCalculationId(crn: String, calculationId: UUID): TierDto? =
    getTierCalculationById(crn, calculationId)?.let {
      TierDto.from(it)
    }.also { log.info("Returned tier for $crn and $calculationId") }

  fun calculateTierForCrn(crn: String): CalculationResultDto {
    val existingTier = getLatestTierCalculation(crn)?.data

    return calculateTier(crn).let {
      CalculationResultDto(TierDto.from(it), it.data != existingTier)
    }.also {
      telemetryService.trackTierCalculated(crn, it)
    }
  }

  private fun calculateTier(crn: String): TierCalculationEntity {

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
      uuid = UUID.randomUUID(),
      created = LocalDateTime.now(clock),
      data = TierCalculationResultEntity(change = changeLevel, protect = protectLevel)
    )

    return tierCalculationRepository.save(calculation).also {
      log.info("Calculated tier for $crn")
    }
  }

  private fun getLatestTierCalculation(crn: String): TierCalculationEntity? =
    tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn).also {
      when (it) {
        null -> log.info("No tier calculation found for $crn")
        else -> log.info("Found latest tier calculation for $crn")
      }
    }

  private fun getTierCalculationById(crn: String, calculationId: UUID): TierCalculationEntity? =
    tierCalculationRepository.findByCrnAndUuid(crn, calculationId).also {
      when (it) {
        null -> log.info("No tier calculation found for $crn $calculationId")
      }
    }

  companion object {
    private val log = LoggerFactory.getLogger(TierCalculationService::class.java)
  }
}
