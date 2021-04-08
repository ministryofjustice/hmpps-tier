package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
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
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor.IOM_NOMINAL

@Service
class ChangeLevelCalculator(
  private val mandateForChange: MandateForChange,
  private val assessmentApiService: AssessmentApiService,
) {

  fun calculateChangeLevel(
    crn: String,
    offenderAssessment: OffenderAssessment?,
    deliusAssessments: DeliusAssessments?,
    registrations: Collection<Registration>,
    convictions: Collection<Conviction>
  ): TierLevel<ChangeLevel> {
    return when {
      mandateForChange.hasNoMandate(crn, convictions) -> TIER_NO_MANDATE
      hasNoAssessment(crn, offenderAssessment) -> TIER_NO_ASSESSMENT

      else -> {
        val orderedRegistrations = registrations
          .filter { it.active }
          .sortedByDescending { it.startDate }

        val points = mapOf(
          NEEDS to getAssessmentNeedsPoints(offenderAssessment),
          OGRS to getOgrsPoints(deliusAssessments),
          IOM to getIomNominalPoints(orderedRegistrations)
        )

        val total = points.map { it.value }.sum()

        when {
          total >= 20 -> TierLevel(THREE, total, points)
          total in 10..19 -> TierLevel(TWO, total, points)
          else -> TierLevel(ONE, total, points)
        }
      }
    }.also { log.debug("Calculated Change Level for $crn: $it") }
  }

  private fun hasNoAssessment(crn: String, offenderAssessment: OffenderAssessment?): Boolean =
    (offenderAssessment == null).also { log.info("Valid assessment found for $crn : $it") }

  private fun getAssessmentNeedsPoints(offenderAssessment: OffenderAssessment?): Int =
    (
      offenderAssessment?.let { assessment ->
        assessmentApiService.getAssessmentNeeds(assessment.assessmentId)
          .let {
            it.entries.sumBy { ent ->
              ent.key.weighting.times(ent.value?.score ?: 0)
            }
          }
      } ?: 0
      ).also { log.debug("Needs Points: $it") }

  private fun getOgrsPoints(deliusAssessments: DeliusAssessments?): Int =
    (deliusAssessments?.ogrs?.div(10) ?: 0)
      .also { log.debug("Ogrs Points: $it") }

  private fun getIomNominalPoints(registrations: Collection<Registration>): Int =
    when {
      registrations.any { ComplexityFactor.from(it.type.code) == IOM_NOMINAL } -> 2
      else -> 0
    }.also { log.debug("IOM Nominal Points: $it") }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val TIER_NO_MANDATE = TierLevel(ZERO, 0, mapOf(NO_MANDATE_FOR_CHANGE to 0))
    private val TIER_NO_ASSESSMENT = TierLevel(TWO, 0, mapOf(NO_VALID_ASSESSMENT to 0))
  }
}
