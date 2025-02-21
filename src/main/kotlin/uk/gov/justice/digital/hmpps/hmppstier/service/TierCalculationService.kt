package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.AssessmentForTier
import uk.gov.justice.digital.hmpps.hmppstier.client.NeedSection
import uk.gov.justice.digital.hmpps.hmppstier.client.SectionAnswer
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.*
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import uk.gov.justice.digital.hmpps.hmppstier.dto.TierDto
import uk.gov.justice.digital.hmpps.hmppstier.exception.CrnNotFoundException
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierSummaryRepository
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
    private val tierSummaryRepository: TierSummaryRepository,
    @Value("\${tier.unsupervised.suffix}") private val includeSuffix: Boolean,
) {
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

    fun calculateTierForCrn(
        crn: String,
        recalculationSource: RecalculationSource,
        allowUpdates: Boolean
    ): TierCalculationEntity? {
        try {
            val tierCalculation = calculateTier(crn, recalculationSource)
            if (allowUpdates) {
                val isUpdated = tierUpdater.updateTier(tierCalculation, crn)
                successUpdater.update(crn, tierCalculation.uuid)
                telemetryService.trackTierCalculated(tierCalculation, isUpdated, recalculationSource)
            } else {
                val currentTier = tierSummaryRepository.findByIdOrNull(crn)?.let { TierDto.from(it, includeSuffix) }
                telemetryService.trackEvent(
                    TIER_RECALCULATION_DRY_RUN,
                    mapOf(
                        "crn" to crn,
                        "currentTier" to currentTier?.tierScore,
                        "calculatedTier" to "${tierCalculation.protectLevel()}${tierCalculation.changeLevel()}"
                    )
                )
            }
            return tierCalculation
        } catch (e: Exception) {
            val eventType = if (allowUpdates) TIER_CALCULATION_FAILED else TIER_RECALCULATION_DRY_RUN_FAILURE
            telemetryService.trackEvent(
                eventType,
                mapOf(
                    "crn" to crn,
                    "exception" to e.message,
                    "recalculationSource" to recalculationSource::class.simpleName,
                    "recalculationReason" to if (recalculationSource is RecalculationSource.EventSource) recalculationSource.type else "",
                    "duplicateAttempt" to (e is DataIntegrityViolationException).toString()
                ),
            )
            if (allowUpdates) {
                checkForCrnNotFound(crn, e)
            }
            return null
        }
    }

    private fun checkForCrnNotFound(crn: String, e: Exception) {
        when (e) {
            is DataIntegrityViolationException -> return
            is CrnNotFoundException -> deleteCalculationsForCrn(crn, "Not Found in Delius")
            else -> throw e
        }
    }

    private fun calculateTier(crn: String, recalculationSource: RecalculationSource): TierCalculationEntity {
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
            changeReason = recalculationSource.changeReason
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

    private fun NeedSection.mapSeverity(): Pair<Need, NeedSeverity>? = getSeverity()?.let { section to it }
}
