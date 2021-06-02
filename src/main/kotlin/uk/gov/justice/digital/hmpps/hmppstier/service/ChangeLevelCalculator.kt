package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.domain.Conviction
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
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity

@Service
class ChangeLevelCalculator(
  private val mandateForChange: MandateForChange,
) {

  fun calculateChangeLevel(
    crn: String,
    offenderAssessment: OffenderAssessment?,
    ogrsScore: Int,
    hasIomNominal: Boolean,
    convictions: Collection<Conviction>,
    needs: Map<Need, NeedSeverity>
  ): TierLevel<ChangeLevel> =
    when {
      mandateForChange.hasNoMandate(crn, convictions) -> TIER_NO_MANDATE
      hasNoAssessment(offenderAssessment) -> TIER_NO_ASSESSMENT

      else -> {
        val points = mapOf(
          NEEDS to getAssessmentNeedsPoints(needs),
          OGRS to getOgrsPoints(ogrsScore),
          IOM to getIomNominalPoints(hasIomNominal)
        )

        val total = points.map { it.value }.sum()

        when {
          total >= 20 -> TierLevel(THREE, total, points)
          total in 10..19 -> TierLevel(TWO, total, points)
          else -> TierLevel(ONE, total, points)
        }
      }
    }

  private fun hasNoAssessment(offenderAssessment: OffenderAssessment?): Boolean =
    (offenderAssessment == null)

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

  companion object {
    private val TIER_NO_MANDATE = TierLevel(ZERO, 0, mapOf(NO_MANDATE_FOR_CHANGE to 0))
    private val TIER_NO_ASSESSMENT = TierLevel(TWO, 0, mapOf(NO_VALID_ASSESSMENT to 0))
  }
}
