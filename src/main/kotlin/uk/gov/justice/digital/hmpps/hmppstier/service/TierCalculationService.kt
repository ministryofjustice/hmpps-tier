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
    private val tierToDeliusApiService: TierToDeliusApiService,
    private val successUpdater: SuccessUpdater,
    private val telemetryService: TelemetryService,
    private val tierUpdater: TierUpdater,
) {

    private val protectLevelCalculator: ProtectLevelCalculator = ProtectLevelCalculator()
    private val changeLevelCalculator: ChangeLevelCalculator = ChangeLevelCalculator()
    private val additionalFactorsForWomen: AdditionalFactorsForWomen = AdditionalFactorsForWomen(assessmentApiService)

    suspend fun calculateTierForCrn(crn: String, recalculationSource: RecalculationSource): Unit = try {
        calculateTier(crn).let {
            val isUpdated = tierUpdater.updateTier(it, crn)
            successUpdater.update(crn, it.uuid)
            telemetryService.trackTierCalculated(it, isUpdated, recalculationSource)
            log.info("Tier calculated for $crn. Different from previous tier: $isUpdated from listener: ${recalculationSource.name}.")
        }
    } catch (e: Exception) {
        log.error("Unable to calculate tier for $crn", e)
        telemetryService.trackEvent(
            TelemetryEventType.TIER_CALCULATION_FAILED,
            mapOf("crn" to crn, "exception" to e.message, "recalculationReason" to recalculationSource.name),
        )
    }

    private suspend fun calculateTier(crn: String): TierCalculationEntity {
        val deliusInputs = tierToDeliusApiService.getTierToDelius(crn)
        val offenderAssessment = assessmentApiService.getRecentAssessment(crn)

        val additionalFactorsPoints = additionalFactorsForWomen.calculate(
            offenderAssessment,
            deliusInputs.isFemale,
            deliusInputs.previousEnforcementActivity,
        )

        val protectLevel =
            protectLevelCalculator.calculate(deliusInputs.rsrScore, additionalFactorsPoints, deliusInputs.registrations)
        val changeLevel = changeLevelCalculator.calculate(
            deliusInputs,
            assessmentApiService.getAssessmentNeeds(offenderAssessment),
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
