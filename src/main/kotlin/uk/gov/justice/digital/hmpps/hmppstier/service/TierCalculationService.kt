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
import uk.gov.justice.digital.hmpps.hmppstier.exception.CrnNotFoundException
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import uk.gov.justice.digital.hmpps.hmppstier.service.TelemetryEventType.*
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
    private val tierReader: TierReader
) {
    fun deleteCalculationsForCrn(crn: String, reason: String) = try {
        tierUpdater.removeTierCalculationsFor(crn)
        telemetryService.trackEvent(
            TIER_CALCULATION_REMOVED,
            mapOf("crn" to crn, "reason" to reason),
        )
    } catch (e: Exception) {
        log.error("Unable to remove tier calculations for $crn")
        telemetryService.trackEvent(
            TIER_CALCULATION_REMOVAL_FAILED,
            mapOf("crn" to crn, "reasonToDelete" to reason, "failureReason" to e.message),
        )
    }

    fun calculateTierForCrn(crn: String, recalculationSource: RecalculationSource, allowUpdates: Boolean) {
        try {
            val tierCalculation = calculateTier(crn)
            if (allowUpdates) {
                val isUpdated = tierUpdater.updateTier(tierCalculation, crn)
                successUpdater.update(crn, tierCalculation.uuid)
                telemetryService.trackTierCalculated(tierCalculation, isUpdated, recalculationSource)
            } else {
                val currentTier = tierReader.getLatestTierByCrn(crn)
                telemetryService.trackEvent(
                    TIER_RECALCULATION_DRY_RUN,
                    mapOf(
                        "currentTier" to currentTier?.tierScore,
                        "calculatedTier" to "${tierCalculation.protectLevel()}${tierCalculation.changeLevel()}"
                    )
                )
            }
        } catch (e: Exception) {
            val eventType = if (allowUpdates) TIER_CALCULATION_FAILED else TIER_RECALCULATION_DRY_RUN_FAILURE
            telemetryService.trackEvent(
                eventType,
                mapOf("crn" to crn, "exception" to e.message, "recalculationReason" to recalculationSource.name),
            )
            if (allowUpdates) {
                checkForCrnNotFound(crn, e)
            }
        }
    }

    private fun checkForCrnNotFound(crn: String, e: Exception) {
        if (e is CrnNotFoundException) {
            deleteCalculationsForCrn(crn, "Not Found in Delius")
        } else {
            throw e
        }
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
            assessment?.assessment == null,
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
        educationTrainingEmployability?.mapSeverity(),
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

    private fun NeedSection.mapSeverity(): Pair<Need, NeedSeverity>? = severity?.let { section to it }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
