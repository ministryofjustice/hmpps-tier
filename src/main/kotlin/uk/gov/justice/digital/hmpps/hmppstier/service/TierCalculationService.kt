package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
  private val assessmentApiService: AssessmentApiService,
  private val communityApiService: CommunityApiService,
  private val successUpdater: SuccessUpdater,
  private val telemetryService: TelemetryService,
  private val additionalFactorsForWomen: AdditionalFactorsForWomen
) {

  private val protectLevelCalculator: ProtectLevelCalculator = ProtectLevelCalculator()

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
      val isUpdated = isUpdated(it, crn)
      tierCalculationRepository.save(it)
      when {
        isUpdated -> successUpdater.update(crn, it.uuid)
      }
      log.info("Tier calculated for $crn. Different from previous tier: $isUpdated")
      telemetryService.trackTierCalculated(it, isUpdated)
    }

  private fun isUpdated(
    it: TierCalculationEntity,
    crn: String
  ): Boolean {
    val latestTierCalculation = getLatestTierCalculation(crn)
    return it.data.protect.tier != latestTierCalculation?.data?.protect?.tier || it.data.change.tier != latestTierCalculation.data.change.tier
  }

  private fun calculateTier(crn: String): TierCalculationEntity {

    val offenderAssessment = assessmentApiService.getRecentAssessment(crn)
    val (rsr, ogrs) = communityApiService.getDeliusAssessments(crn)
    val registrations = communityApiService.getRegistrations(crn)
    val deliusConvictions = communityApiService.getConvictionsWithSentences(crn)

    val protectLevel = protectLevelCalculator.calculateProtectLevel(
      rsr,
      additionalFactorsForWomen.calculate(crn, deliusConvictions, offenderAssessment),
      registrations
    )
    val changeLevel = changeLevelCalculator.calculateChangeLevel(
      crn,
      offenderAssessment,
      ogrs,
      registrations.iomNominal,
      deliusConvictions,
      assessmentApiService.getAssessmentNeeds(offenderAssessment)
    )

    return TierCalculationEntity(
      crn = crn,
      created = LocalDateTime.now(clock),
      data = TierCalculationResultEntity(
        change = changeLevel,
        protect = protectLevel,
        calculationVersion = "2"
      )
    )
  }

  private fun getLatestTierCalculation(crn: String): TierCalculationEntity? =
    tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn)

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
