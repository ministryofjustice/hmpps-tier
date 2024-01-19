package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.AssessmentForTier
import uk.gov.justice.digital.hmpps.hmppstier.client.NeedSection
import uk.gov.justice.digital.hmpps.hmppstier.client.SectionAnswer
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.*
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
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
    fun deleteCalculationsForCrn(crn: String, reason: String) = try {
        tierUpdater.removeTierCalculationsFor(crn)
        telemetryService.trackEvent(
            TelemetryEventType.TIER_CALCULATION_REMOVED,
            mapOf("crn" to crn, "reason" to reason),
        )
    } catch (e: Exception) {
        log.error("Unable to remove tier calculations for $crn")
        telemetryService.trackEvent(
            TelemetryEventType.TIER_CALCULATION_REMOVAL_FAILED,
            mapOf("crn" to crn, "reasonToDelete" to reason, "failureReason" to e.message),
        )
    }

    fun calculateTierForCrn(crn: String, recalculationSource: RecalculationSource): Unit = try {
        val tierCalculation = calculateTier(crn)
        val isUpdated = tierUpdater.updateTier(tierCalculation, crn)
        successUpdater.update(crn, tierCalculation.uuid)
        telemetryService.trackTierCalculated(tierCalculation, isUpdated, recalculationSource)
    } catch (e: Exception) {
        log.error("Unable to calculate tier for $crn", e)
        telemetryService.trackEvent(
            TelemetryEventType.TIER_CALCULATION_FAILED,
            mapOf("crn" to crn, "exception" to e.message, "recalculationReason" to recalculationSource.name),
        )
    }

    private fun calculateTier(crn: String): TierCalculationEntity {
        val deliusInputs = tierToDeliusApiService.getTierToDelius(crn)
        val assessment = assessmentApiService.getTierAssessmentInformation(crn)

        val additionalFactorsPoints = AdditionalFactorsForWomen.calculate(
            assessment?.additionalFactorsForWomen(),
            deliusInputs.isFemale,
            deliusInputs.previousEnforcementActivity,
        )

        val protectLevel = ProtectLevelCalculator.calculate(
            deliusInputs.rsrScore, additionalFactorsPoints, deliusInputs.registrations,
        )
        val changeLevel = ChangeLevelCalculator.calculate(
            deliusInputs,
            assessment?.mapNeedsAndSeverities() ?: mapOf(),
            assessment == null,
        )

        return TierCalculationEntity(
            crn = crn,
            created = LocalDateTime.now(clock),
            data = TierCalculationResultEntity(
                change = changeLevel,
                protect = protectLevel,
                calculationVersion = "2",
                deliusInputs = deliusInputs,
                assessmentSummary = assessment
            ),
        )
    }

    private fun AssessmentForTier.mapNeedsAndSeverities() = listOfNotNull(
        accommodation?.mapSeverity(),
        educationTrainingEmployment?.mapSeverity(),
        relationships?.mapSeverity(),
        lifestyleAndAssociates?.mapSeverity(),
        drugMisuse?.mapSeverity(),
        alcoholMisuse?.mapSeverity(),
        thinkingAndBehaviour?.mapSeverity(),
        attitudes?.mapSeverity(),
    ).toMap()

    private fun AssessmentForTier.additionalFactorsForWomen(): Map<AdditionalFactorForWomen, SectionAnswer> =
        listOfNotNull(
            relationships?.parentalResponsibilities?.let { PARENTING_RESPONSIBILITIES to it },
            thinkingAndBehaviour?.impulsivity?.let { IMPULSIVITY to it },
            thinkingAndBehaviour?.temperControl?.let { TEMPER_CONTROL to it }
        ).toMap()

    private fun NeedSection.mapSeverity(): Pair<Need, NeedSeverity> = section to severity

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
