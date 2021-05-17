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
  private val communityApiService: CommunityApiService,
  private val communityApiClient: CommunityApiClient, // Deprecated
  private val successUpdater: SuccessUpdater,
  private val telemetryService: TelemetryService,
  @Value("\${calculation.version}")private val calculationVersion: Int
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
    val deliusAssessments = communityApiService.getDeliusAssessments(crn)
    val (iomNominal, otherRegistrations) = communityApiClient.getRegistrations(crn)
    val deliusConvictions = communityApiClient.getConvictionsWithSentences(crn)
    val needs = assessmentApiService.getAssessmentNeeds(offenderAssessment)

    val protectLevel = protectLevelCalculator.calculateProtectLevel(
      crn,
      offenderAssessment,
      deliusAssessments.rsr,
      otherRegistrations,
      deliusConvictions
    )
    val changeLevel = changeLevelCalculator.calculateChangeLevel(
      crn,
      offenderAssessment,
      deliusAssessments.ogrs,
      iomNominal,
      deliusConvictions,
      needs
    )

    return TierCalculationEntity(
      crn = crn,
      created = LocalDateTime.now(clock),
      data = TierCalculationResultEntity(change = changeLevel, protect = protectLevel, calculationVersion = calculationVersion.toString())
    )
  }

  private fun getLatestTierCalculation(crn: String): TierCalculationEntity? =
    tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn)

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
