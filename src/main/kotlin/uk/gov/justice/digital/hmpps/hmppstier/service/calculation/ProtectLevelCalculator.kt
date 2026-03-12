package uk.gov.justice.digital.hmpps.hmppstier.service.calculation

import uk.gov.justice.digital.hmpps.hmppstier.client.arns.AssessmentForTier
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusInputs
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.CalculationRule.*
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.MappaLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.MappaLevel.*
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel.*
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh.*
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.RsrThresholds.*
import uk.gov.justice.digital.hmpps.hmppstier.service.calculation.AdditionalFactorsForWomen.additionalFactorsForWomen
import java.math.BigDecimal

@Deprecated("Single tier value provided by TierCalculator", ReplaceWith("TierCalculator"))
object ProtectLevelCalculator {

    fun calculate(
        deliusInputs: DeliusInputs,
        assessment: AssessmentForTier?,
    ): TierLevel<ProtectLevel> {
        val rsr = deliusInputs.rsrScore
        val registrations = deliusInputs.registrations
        val additionalFactorsPoints = AdditionalFactorsForWomen.calculate(
            assessment?.additionalFactorsForWomen(),
            deliusInputs.isFemale,
            deliusInputs.previousEnforcementActivity,
        )

        val points = mapOf(
            RSR to getRsrPoints(rsr),
            ROSH to getRoshPoints(registrations.rosh),
            MAPPA to getMappaPoints(registrations.mappaLevel),
            COMPLEXITY to getComplexityPoints(registrations.complexityFactors),
            ADDITIONAL_FACTORS_FOR_WOMEN to additionalFactorsPoints,
        )

        val total = points.map { it.value }.sum()
            .minus(minOf(points.getOrDefault(RSR, 0), points.getOrDefault(ROSH, 0)))

        return when {
            total >= LEVEL_A_LOWER_THRESHOLD -> TierLevel(A, total, points)
            total in 20 until LEVEL_A_LOWER_THRESHOLD -> TierLevel(B, total, points)
            total in 10 until 20 -> TierLevel(C, total, points)
            else -> TierLevel(D, total, points)
        }
    }

    private fun getRsrPoints(rsr: BigDecimal): Int =
        when (rsr) {
            in TIER_B_RSR_LOWER.num..TIER_B_RSR_UPPER.num -> 20
            in TIER_C_RSR_LOWER.num..TIER_C_RSR_UPPER.num -> 10
            else -> 0
        }

    private fun getRoshPoints(rosh: Rosh?): Int =
        when (rosh) {
            VERY_HIGH -> LEVEL_A_LOWER_THRESHOLD
            HIGH -> 20
            MEDIUM -> 10
            else -> 0
        }

    private fun getMappaPoints(mappaLevel: MappaLevel?): Int =
        when (mappaLevel) {
            M3, M2 -> LEVEL_A_LOWER_THRESHOLD
            M1 -> 5
            else -> 0
        }

    private fun getComplexityPoints(complexityFactors: Collection<ComplexityFactor>): Int =
        complexityFactors.count().times(2)

    private const val LEVEL_A_LOWER_THRESHOLD = 150
}
