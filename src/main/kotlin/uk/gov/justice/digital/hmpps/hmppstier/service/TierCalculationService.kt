package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.AssessmentForTier
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.AssessmentSummary
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.NeedSection
import uk.gov.justice.digital.hmpps.hmppstier.domain.RecalculationSource
import uk.gov.justice.digital.hmpps.hmppstier.domain.TelemetryEventType.*
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import uk.gov.justice.digital.hmpps.hmppstier.exception.CrnNotFoundException
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import uk.gov.justice.digital.hmpps.hmppstier.messaging.publisher.DomainEventPublisher
import uk.gov.justice.digital.hmpps.hmppstier.service.api.AssessmentApiService
import uk.gov.justice.digital.hmpps.hmppstier.service.api.DeliusApiService
import uk.gov.justice.digital.hmpps.hmppstier.service.calculation.ChangeLevelCalculator
import uk.gov.justice.digital.hmpps.hmppstier.service.calculation.ProtectLevelCalculator
import uk.gov.justice.digital.hmpps.hmppstier.service.calculation.TierCalculator
import java.time.Clock
import java.time.LocalDateTime

@Service
class TierCalculationService(
    private val clock: Clock,
    private val assessmentApiService: AssessmentApiService,
    private val deliusApiService: DeliusApiService,
    private val domainEventPublisher: DomainEventPublisher,
    private val telemetryService: TelemetryService,
    private val tierUpdater: TierUpdater,
) {
    fun calculateTierForCrn(
        crn: String,
        recalculationSource: RecalculationSource
    ): TierCalculationEntity? {
        try {
            val tierCalculation = calculateTier(crn, recalculationSource)
            val isUpdated = tierUpdater.updateTier(tierCalculation, crn)
            domainEventPublisher.update(crn, isUpdated, tierCalculation.uuid)
            telemetryService.trackTierCalculated(tierCalculation, isUpdated, recalculationSource)
            return tierCalculation
        } catch (e: Exception) {
            telemetryService.trackEvent(
                TIER_CALCULATION_FAILED,
                mapOf(
                    "crn" to crn,
                    "exception" to e.message,
                    "recalculationSource" to recalculationSource::class.simpleName,
                    "recalculationReason" to if (recalculationSource is RecalculationSource.EventSource) recalculationSource.type else "",
                    "duplicateAttempt" to (e is DataIntegrityViolationException).toString()
                ),
            )
            checkForCrnNotFound(crn, e)
            return null
        }
    }

    fun deleteCalculationsForCrn(crn: String, reason: String) = try {
        tierUpdater.removeTierCalculationsFor(crn)
        telemetryService.trackEvent(
            TIER_CALCULATION_REMOVED,
            mapOf("crn" to crn, "reason" to reason),
        )
    } catch (e: Exception) {
        telemetryService.trackEvent(
            TIER_CALCULATION_REMOVAL_FAILED,
            mapOf("crn" to crn, "reasonToDelete" to reason, "failureReason" to e.message),
        )
        throw e
    }

    private fun checkForCrnNotFound(crn: String, e: Exception) {
        when (e) {
            is DataIntegrityViolationException -> return
            is CrnNotFoundException -> deleteCalculationsForCrn(crn, "Not Found in Delius")
            else -> throw e
        }
    }

    private fun calculateTier(crn: String, recalculationSource: RecalculationSource): TierCalculationEntity {
        val deliusInputs = deliusApiService.getTierToDelius(crn)
        val assessment = assessmentApiService.getTierAssessmentInformation(crn)
        val predictors = assessmentApiService.getRiskPredictors(crn)

        // Old tier - protect axis (A-D) + change axis (0-3) - used primarily for allocation
        val protectLevel = ProtectLevelCalculator.calculate(
            deliusInputs, assessment
        )
        val changeLevel = ChangeLevelCalculator.calculate(
            deliusInputs,
            assessment?.mapNeedsAndSeverities() ?: mapOf(),
            assessment?.assessment == null,
        )

        // New tier - single axis (A-G) - used for allocation and supervision packages
        val tier = TierCalculator.calculate(deliusInputs, predictors?.output)

        return TierCalculationEntity(
            crn = crn,
            created = LocalDateTime.now(clock),
            data = TierCalculationResultEntity(
                tier = tier,
                change = changeLevel,
                protect = protectLevel,
                calculationVersion = "3",
                deliusInputs = deliusInputs,
                assessmentSummary = assessment,
                riskPredictors = predictors
            ),
            changeReason = recalculationSource.changeReason
        )
    }

    private fun AssessmentForTier.mapNeedsAndSeverities(): Map<Need, NeedSeverity> {
        val san = assessment.isSanAssessment()
        return listOfNotNull(
            accommodation?.mapSeverity(san),
            educationTrainingEmployability?.mapSeverity(san),
            relationships?.mapSeverity(san),
            lifestyleAndAssociates?.mapSeverity(san),
            drugMisuse?.mapSeverity(san),
            alcoholMisuse?.mapSeverity(san),
            thinkingAndBehaviour?.mapSeverity(san),
            attitudes?.mapSeverity(san),
        ).toMap()
    }

    private fun NeedSection.mapSeverity(sanIndicator: Boolean): Pair<Need, NeedSeverity>? =
        getSeverity(sanIndicator)?.let { section to it }

    private fun AssessmentSummary?.isSanAssessment() = this?.sanIndicator == true
}
