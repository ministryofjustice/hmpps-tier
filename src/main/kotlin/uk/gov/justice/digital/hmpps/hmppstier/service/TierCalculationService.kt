package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
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
  private val successUpdater: SuccessUpdater,
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

  @Transactional
  fun calculateTierForCrn(crn: String) {
    val newTier = calculateTier(crn)
    val existingTier = getLatestTierCalculation(crn)
    val isUpdated = newTier.data != existingTier?.data
    tierCalculationRepository.save(newTier)
    if (isUpdated) {
      successUpdater.update(crn, newTier.uuid)
    }
    log.info("Tier calculated for $crn. Different from previous tier: $isUpdated")
    telemetryService.trackTierCalculated(crn, newTier, isUpdated)
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

    return TierCalculationEntity(
      crn = crn,
      uuid = UUID.randomUUID(),
      created = LocalDateTime.now(clock),
      data = TierCalculationResultEntity(change = changeLevel, protect = protectLevel)
    ).also {
      log.info("Calculated tier for $crn")
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

  private fun getTierCalculationById(crn: String, calculationId: UUID): TierCalculationEntity? {
    log.debug("Finding latest tier calculation for $crn")

    return tierCalculationRepository.findByCrnAndUuid(crn, calculationId).also {
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
