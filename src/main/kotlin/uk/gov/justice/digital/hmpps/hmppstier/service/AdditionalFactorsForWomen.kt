package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.domain.Sentence
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.IMPULSIVITY
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.TEMPER_CONTROL
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.OffenceCode
import java.time.Clock
import java.time.LocalDate

@Service
class AdditionalFactorsForWomen(
  private val clock: Clock,
  private val assessmentApiService: AssessmentApiService,
  private val communityApiService: CommunityApiService
) {
  fun calculate(
    crn: String,
    convictions: Collection<Conviction>,
    offenderAssessment: OffenderAssessment?,
    offenderIsFemale: Boolean
  ): Int =
    when {
      offenderIsFemale -> {
        val additionalFactorsPoints = getAdditionalFactorsAssessmentComplexityPoints(offenderAssessment)
        val breachRecallPoints = getBreachRecallComplexityPoints(crn, convictions)
        val harassmentPoints = getHarassmentPoints(convictions)

        additionalFactorsPoints + breachRecallPoints + harassmentPoints
      }
      else -> 0
    }

  private fun getHarassmentPoints(convictions: Collection<Conviction>): Int =
    if (convictions.flatMap { it.offenceCodes }.any { OFFENCE_CODES.contains(it) }) 2 else 0

  private fun getAdditionalFactorsAssessmentComplexityPoints(offenderAssessment: OffenderAssessment?): Int =
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

  private fun getBreachRecallComplexityPoints(crn: String, convictions: Collection<Conviction>): Int =
    convictions
      .filter { qualifyingConvictions(it.sentence) }
      .let {
        when {
          communityApiService.hasBreachedConvictions(crn, it) -> 2
          else -> 0
        }
      }

  private fun isYes(value: String?): Boolean =
    value.equals("YES", true) || value.equals("Y", true)

  private fun isAnswered(value: String?): Boolean =
    value?.toInt() ?: 0 > 0

  private fun qualifyingConvictions(sentence: Sentence): Boolean =
    sentence.terminationDate == null ||
      sentence.terminationDate.isAfter(LocalDate.now(clock).minusYears(1).minusDays(1))

  companion object {
    val OFFENCE_CODES = OffenceCode.values().map { it.code }
  }
}
