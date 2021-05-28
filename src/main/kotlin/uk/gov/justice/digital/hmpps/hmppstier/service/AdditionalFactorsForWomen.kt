package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.domain.Sentence
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.IMPULSIVITY
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.TEMPER_CONTROL
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NsiOutcome
import java.time.Clock
import java.time.LocalDate

@Service
class AdditionalFactorsForWomen(
  private val clock: Clock,
  private val communityApiClient: CommunityApiClient,
  private val assessmentApiService: AssessmentApiService,
) {
  fun calculate(
    crn: String,
    convictions: Collection<Conviction>,
    offenderAssessment: OffenderAssessment?
  ): Int =
    when {
      isFemale(crn) -> {
        val additionalFactorsPoints = getAdditionalFactorsAssessmentComplexityPoints(offenderAssessment)
        val breachRecallPoints = getBreachRecallComplexityPoints(crn, convictions)

        additionalFactorsPoints + breachRecallPoints
      }
      else -> 0
    }

  private fun isFemale(crn: String) = communityApiClient.getOffender(crn)?.gender.equals("female", true)

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
          it.any { conviction -> convictionHasBreachOrRecallNsis(crn, conviction.convictionId) } -> 2
          else -> 0
        }
      }

  private fun isYes(value: String?): Boolean =
    value.equals("YES", true) || value.equals("Y", true)

  private fun isAnswered(value: String?): Boolean =
    value?.toInt() ?: 0 > 0

  private fun convictionHasBreachOrRecallNsis(crn: String, convictionId: Long): Boolean =
    communityApiClient.getBreachRecallNsis(crn, convictionId)
      .any { NsiOutcome.from(it.status?.code) != null }

  private fun qualifyingConvictions(sentence: Sentence): Boolean =
    sentence.terminationDate == null ||
      sentence.terminationDate.isAfter(LocalDate.now(clock).minusYears(1).minusDays(1))
}
