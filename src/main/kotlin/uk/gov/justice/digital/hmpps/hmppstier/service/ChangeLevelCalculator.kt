package uk.gov.justice.digital.hmpps.hmppstier.service

import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusInputs
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.CalculationRule.*
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel.*
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity

object ChangeLevelCalculator {

    fun calculate(
        deliusInputs: DeliusInputs,
        needs: Map<Need, NeedSeverity>,
        hasNoAssessment: Boolean,
    ): TierLevel<ChangeLevel> =
        when {
            deliusInputs.hasNoMandate -> TIER_NO_MANDATE
            hasNoAssessment -> TIER_NO_ASSESSMENT

            else -> {
                val points = mapOf(
                    NEEDS to getAssessmentNeedsPoints(needs),
                    OGRS to getOgrsPoints(deliusInputs.ogrsScore),
                    IOM to getIomNominalPoints(deliusInputs.registrations.hasIomNominal),
                )

                val total = points.map { it.value }.sum()

                when {
                    total >= 20 -> TierLevel(THREE, total, points)
                    total in 10..19 -> TierLevel(TWO, total, points)
                    else -> TierLevel(ONE, total, points)
                }
            }
        }

    private fun getAssessmentNeedsPoints(needs: Map<Need, NeedSeverity>): Int =
        needs.entries.sumOf {
            it.key.weighting.times(it.value.score)
        }

    private fun getOgrsPoints(ogrsScore: Int): Int =
        ogrsScore.div(10)

    private fun getIomNominalPoints(hasIomNominal: Boolean): Int =
        when {
            hasIomNominal -> 2
            else -> 0
        }

    private val TIER_NO_MANDATE = TierLevel(ZERO, 0, mapOf(NO_MANDATE_FOR_CHANGE to 0))
    private val TIER_NO_ASSESSMENT = TierLevel(TWO, 0, mapOf(NO_VALID_ASSESSMENT to 0))
}
