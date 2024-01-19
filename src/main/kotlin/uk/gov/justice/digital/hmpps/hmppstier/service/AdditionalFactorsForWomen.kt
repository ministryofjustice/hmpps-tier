package uk.gov.justice.digital.hmpps.hmppstier.service

import uk.gov.justice.digital.hmpps.hmppstier.client.SectionAnswer
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.*

object AdditionalFactorsForWomen {
    fun calculate(
        additionalFactorForWomen: Map<AdditionalFactorForWomen, SectionAnswer>?,
        offenderIsFemale: Boolean,
        previousEnforcementActivity: Boolean,
    ): Int =
        when {
            offenderIsFemale -> {
                val additionalFactorsPoints =
                    additionalFactorForWomen?.let { getAdditionalFactorsAssessmentComplexityPoints(it) } ?: 0
                val breachRecallPoints = getBreachRecallComplexityPoints(previousEnforcementActivity)

                additionalFactorsPoints + breachRecallPoints
            }

            else -> 0
        }

    private fun getAdditionalFactorsAssessmentComplexityPoints(additionalFactors: Map<AdditionalFactorForWomen, SectionAnswer>): Int =
        additionalFactors.let { answers ->
            val parenting = if (isAnswered(answers[PARENTING_RESPONSIBILITIES])) 1 else 0
            // We don't take the cumulative score, just '1' if at least one of these two is present
            val selfControl = if (isAnswered(answers[IMPULSIVITY]) || isAnswered(answers[TEMPER_CONTROL])) 1 else 0
            (parenting + selfControl).times(2)
        }

    private fun getBreachRecallComplexityPoints(previousEnforcementActivity: Boolean): Int =
        if (previousEnforcementActivity) {
            2
        } else {
            0
        }

    private fun isAnswered(value: SectionAnswer?): Boolean = (value?.score ?: 0) > 0
}
