package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import java.time.Clock
import java.time.LocalDateTime

@Service
class TierCalculationService(
  private val clock: Clock,
  private val assessmentApiService: AssessmentApiService,
  private val communityApiService: CommunityApiService,
  private val tierToDeliusApiService: TierToDeliusApiService,
  private val successUpdater: SuccessUpdater,
  private val telemetryService: TelemetryService,
  private val tierUpdater: TierUpdater,
) {

  private val protectLevelCalculator: ProtectLevelCalculator = ProtectLevelCalculator()
  private val changeLevelCalculator: ChangeLevelCalculator = ChangeLevelCalculator()
  private val mandateForChange: MandateForChange = MandateForChange(communityApiService)
  private val additionalFactorsForWomen: AdditionalFactorsForWomen = AdditionalFactorsForWomen(clock, assessmentApiService, communityApiService)

  suspend fun calculateTierForCrn(crn: String, listener: String) =
    calculateTier(crn).let {
      val isUpdated = tierUpdater.updateTier(it, crn)
      when {
        isUpdated -> successUpdater.update(crn, it.uuid)
      }
      telemetryService.trackTierCalculated(it, isUpdated)
      log.info("Tier calculated for $crn. Different from previous tier: $isUpdated from listener: $listener.")
    }

  private suspend fun calculateTier(crn: String): TierCalculationEntity {
    val tierToDeliusResponse = tierToDeliusApiService.getTierToDelius(crn)
    val offenderAssessment = assessmentApiService.getRecentAssessment(crn)

    val registrations = communityApiService.getRegistrations(crn)
    val convictions = communityApiService.getConvictionsWithSentences(crn)

    val additionalFactorsPoints = additionalFactorsForWomen.calculate(
      crn,
      convictions,
      offenderAssessment,
      tierToDeliusResponse.gender.equals("female", true),
    )

    val protectLevel = protectLevelCalculator.calculate(tierToDeliusResponse.rsrscore!!, additionalFactorsPoints, registrations)
    val changeLevel = changeLevelCalculator.calculate(
      tierToDeliusResponse.ogrsscore!!,
      registrations.hasIomNominal,
      assessmentApiService.getAssessmentNeeds(offenderAssessment),
      mandateForChange.hasNoMandate(crn, convictions),
      hasNoAssessment(offenderAssessment),
    )

    return TierCalculationEntity(
      crn = crn,
      created = LocalDateTime.now(clock),
      data = TierCalculationResultEntity(
        change = changeLevel,
        protect = protectLevel,
        calculationVersion = "2",
      ),
    )
  }

  private fun hasNoAssessment(offenderAssessment: OffenderAssessment?): Boolean =
    (offenderAssessment == null)
  companion object {
    private val log =
      LoggerFactory.getLogger(this::class.java)
  }
}
