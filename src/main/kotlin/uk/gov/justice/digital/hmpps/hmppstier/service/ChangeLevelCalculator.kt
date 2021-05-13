package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusAssessments
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.client.Registration
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.CalculationRule.IOM
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.CalculationRule.NEEDS
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.CalculationRule.NO_MANDATE_FOR_CHANGE
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.CalculationRule.NO_VALID_ASSESSMENT
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.CalculationRule.OGRS
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel.ONE
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel.THREE
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel.TWO
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel.ZERO

@Service
class ChangeLevelCalculator(
  private val mandateForChange: MandateForChange,
  private val assessmentApiService: AssessmentApiService,
) {

  fun calculateChangeLevel(
    crn: String,
    offenderAssessment: OffenderAssessment?,
    deliusAssessments: DeliusAssessments?,
    iomNominalRegistrations: Collection<Registration>,
    convictions: Collection<Conviction>
  ): TierLevel<ChangeLevel> {
    return when {
      mandateForChange.hasNoMandate(crn, convictions) -> TIER_NO_MANDATE
      hasNoAssessment(offenderAssessment) -> TIER_NO_ASSESSMENT

      else -> {
        val points = mapOf(
          NEEDS to getAssessmentNeedsPoints(offenderAssessment!!.assessmentId),
          OGRS to getOgrsPoints(deliusAssessments),
          IOM to getIomNominalPoints(iomNominalRegistrations)
        )

        val total = points.map { it.value }.sum()

        when {
          total >= 20 -> TierLevel(THREE, total, points)
          total in 10..19 -> TierLevel(TWO, total, points)
          else -> TierLevel(ONE, total, points)
        }
      }
    }
  }

  private fun hasNoAssessment(offenderAssessment: OffenderAssessment?): Boolean =
    (offenderAssessment == null)

  private fun getAssessmentNeedsPoints(assessmentId: String): Int =
    (
      assessmentApiService.getAssessmentNeeds(assessmentId)
        .let {
          it.entries.sumOf { ent ->
            ent.key.weighting.times(ent.value.score)
          }
        }
      )

  private fun getOgrsPoints(deliusAssessments: DeliusAssessments?): Int =
    (deliusAssessments?.ogrs?.div(10) ?: 0)

  private fun getIomNominalPoints(registrations: Collection<Registration>): Int =
    when {
      registrations.any() -> 2
      else -> 0
    }

  companion object {
    private val TIER_NO_MANDATE = TierLevel(ZERO, 0, mapOf(NO_MANDATE_FOR_CHANGE to 0))
    private val TIER_NO_ASSESSMENT = TierLevel(TWO, 0, mapOf(NO_VALID_ASSESSMENT to 0))
  }
}
