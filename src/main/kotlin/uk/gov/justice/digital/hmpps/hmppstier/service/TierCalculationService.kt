package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
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
  private val telemetryService: TelemetryService,
  @Value("\${calculation.version}")private val version: String
) {

  fun getLatestTierByCrn(crn: String): TierDto? =
    TierDto from getLatestTierCalculation(crn)

  fun getTierByCalculationId(crn: String, calculationId: UUID): TierDto? =
    TierDto from getTierCalculationById(crn, calculationId)

  @Transactional
  fun calculateTierForCrn(crn: String) =
    calculateTier(crn).let {
      val isUpdated = it.data != getLatestTierCalculation(crn)?.data
      tierCalculationRepository.save(it)
      when {
        isUpdated -> successUpdater.update(crn, it.uuid)
      }
      log.info("Tier calculated for $crn. Different from previous tier: $isUpdated")
      telemetryService.trackTierCalculated(crn, it, isUpdated)
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

    return TierCalculationEntity(
      crn = crn,
      created = LocalDateTime.now(clock),
      data = TierCalculationResultEntity(change = changeLevel, protect = protectLevel, calculationVersion = version)
    )
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
        null -> log.info("No tier calculation found for $crn and $calculationId")
        else -> log.info("Found tier for $crn and $calculationId")
      }
    }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
