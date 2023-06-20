package uk.gov.justice.digital.hmpps.hmppstier.service

import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.IMPULSIVITY
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.TEMPER_CONTROL

class AdditionalFactorsForWomen(
  private val assessmentApiService: AssessmentApiService,
) {
  suspend fun calculate(
    offenderAssessment: OffenderAssessment?,
    offenderIsFemale: Boolean,
    previousEnforcementActivity: Boolean,
  ): Int =
    when {
      offenderIsFemale -> {
        val additionalFactorsPoints = getAdditionalFactorsAssessmentComplexityPoints(offenderAssessment)
        val breachRecallPoints = getBreachRecallComplexityPoints(previousEnforcementActivity)

        additionalFactorsPoints + breachRecallPoints
      }
      else -> 0
    }

  private suspend fun getAdditionalFactorsAssessmentComplexityPoints(offenderAssessment: OffenderAssessment?): Int =
    when (offenderAssessment) {
      null -> 0
      else -> {
        assessmentApiService.getAssessmentAnswers(offenderAssessment.assessmentId)
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
      }
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
