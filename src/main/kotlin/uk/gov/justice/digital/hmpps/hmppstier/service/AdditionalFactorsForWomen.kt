package uk.gov.justice.digital.hmpps.hmppstier.service

import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.*

object AdditionalFactorsForWomen {
  fun calculate(
    additionalFactorForWomen: Map<AdditionalFactorForWomen, String?>?,
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

  private fun getAdditionalFactorsAssessmentComplexityPoints(additionalFactors: Map<AdditionalFactorForWomen, String?>): Int =
    additionalFactors
      .let { answers ->
        val parenting = when {
          isYes(answers[PARENTING_RESPONSIBILITIES]) -> 1
          else -> 0
        }
        // We dont take the cumulative score, just '1' if at least one of these two is present
        val selfControl = when {
          isAnswered(answers[IMPULSIVITY]) || isAnswered(answers[TEMPER_CONTROL]) -> 1
          else -> 0
        }
        (parenting + selfControl).times(2)
      }

  private fun getBreachRecallComplexityPoints(previousEnforcementActivity: Boolean): Int =
    if (previousEnforcementActivity) {
      2
    } else {
      0
    }

  private fun isYes(value: String?): Boolean =
    value.equals("YES", true) || value.equals("Y", true)

  private fun isAnswered(value: String?): Boolean =
    (value?.toInt() ?: 0) > 0
}
