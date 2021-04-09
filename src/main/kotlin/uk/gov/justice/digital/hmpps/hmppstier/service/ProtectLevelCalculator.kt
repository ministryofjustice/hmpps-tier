package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusAssessments
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.client.Registration
import uk.gov.justice.digital.hmpps.hmppstier.client.Sentence
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.CalculationRule
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NsiOutcome
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.RsrThresholds
import java.time.Clock
import java.time.LocalDate

@Service
class ProtectLevelCalculator(
  private val clock: Clock,
  private val communityApiClient: CommunityApiClient,
  private val assessmentApiService: AssessmentApiService
) {

  fun calculateProtectLevel(
    crn: String,
    offenderAssessment: OffenderAssessment?,
    deliusAssessments: DeliusAssessments?,
    registrations: Collection<Registration>,
    convictions: Collection<Conviction>
  ): TierLevel<ProtectLevel> {

    val points = mapOf(
      CalculationRule.RSR to getRsrPoints(deliusAssessments),
      CalculationRule.ROSH to getRoshPoints(registrations),
      CalculationRule.MAPPA to getMappaPoints(registrations),
      CalculationRule.COMPLEXITY to getComplexityPoints(registrations),
      CalculationRule.ADDITIONAL_FACTORS_FOR_WOMEN to getAdditionalFactorsForWomen(crn, convictions, offenderAssessment)
    )

    val total = points.map { it.value }.sum().minus(minOf(points.getOrDefault(CalculationRule.RSR, 0), points.getOrDefault(CalculationRule.ROSH, 0)))

    return when {
      total >= 30 -> TierLevel(ProtectLevel.A, total, points)
      total in 20..29 -> TierLevel(ProtectLevel.B, total, points)
      total in 10..19 -> TierLevel(ProtectLevel.C, total, points)
      else -> TierLevel(ProtectLevel.D, total, points)
    }.also { log.debug("Calculated Protect Level for $crn: $it") }
  }

  private fun getRsrPoints(deliusAssessments: DeliusAssessments?): Int =
    deliusAssessments?.rsr
      .let { rsr ->
        when {
          rsr != null && rsr >= RsrThresholds.TIER_B_RSR.num -> 20
          rsr != null && rsr >= RsrThresholds.TIER_C_RSR.num -> 10
          else -> 0
        }
      }.also { log.debug("RSR Points: $it") }

  private fun getRoshPoints(registrations: Collection<Registration>): Int =
    registrations
      .mapNotNull { Rosh.from(it.type.code) }
      .firstOrNull()
      .let { rosh ->
        when (rosh) {
          Rosh.VERY_HIGH -> 30
          Rosh.HIGH -> 20
          Rosh.MEDIUM -> 10
          else -> 0
        }
      }.also { log.debug("ROSH Points: $it") }

  private fun getMappaPoints(registrations: Collection<Registration>): Int =
    registrations
      .mapNotNull { Mappa.from(it.registerLevel?.code) }
      .firstOrNull()
      .let { mappa ->
        when (mappa) {
          Mappa.M3, Mappa.M2 -> 30
          Mappa.M1 -> 5
          else -> 0
        }
      }.also { log.debug("MAPPA Points: $it") }

  private fun getComplexityPoints(registrations: Collection<Registration>): Int =
    registrations
      .mapNotNull { ComplexityFactor.from(it.type.code) }
      .distinct()
      .filter { it != ComplexityFactor.IOM_NOMINAL }
      .count()
      .times(2)
      .also { log.debug("Complexity factor size: $it") }

  private fun getAdditionalFactorsForWomen(
    crn: String,
    convictions: Collection<Conviction>,
    offenderAssessment: OffenderAssessment?
  ): Int =
    when {
      isFemale(crn) -> {
        (getAdditionalFactorsAssessmentComplexityPoints(offenderAssessment) + getBreachRecallComplexityPoints(crn, convictions))
          .times(2)
      }
      else -> 0
    }.also { log.debug("Additional Factors for Women for $crn : $it") }

  private fun isFemale(crn: String) = communityApiClient.getOffender(crn)?.gender.equals("female", true)

  private fun getAdditionalFactorsAssessmentComplexityPoints(offenderAssessment: OffenderAssessment?): Int =
    when (offenderAssessment) {
      null -> 0
      else -> {
        assessmentApiService.getAssessmentAnswers(offenderAssessment.assessmentId)
          .also { log.debug("Additional Factors for Women Assessment answers $it ") }
          .let { answers ->
            val parenting = when {
              isYes(answers[AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES]) -> 1
              else -> 0
            }
            // We dont take the cumulative score, just '1' if at least one of these two is present
            val selfControl = when {
              isAnswered(answers[AdditionalFactorForWomen.IMPULSIVITY]) || isAnswered(answers[AdditionalFactorForWomen.TEMPER_CONTROL]) -> 1
              else -> 0
            }
            parenting + selfControl
          }
      }
    }.also { log.debug("Additional Factors for Women Points $it") }

  private fun getBreachRecallComplexityPoints(crn: String, convictions: Collection<Conviction>): Int =
    convictions
      .filter { qualifyingConvictions(it.sentence) }
      .let {
        when {
          it.any { conviction -> convictionHasBreachOrRecallNsis(crn, conviction.convictionId) } -> 1
          else -> 0
        }
      }.also { log.debug("Breach and Recall Complexity Points: $it") }

  private fun isYes(value: String?): Boolean =
    value.equals("YES", true) || value.equals("Y", true)

  private fun isAnswered(value: String?): Boolean =
    value?.toInt() ?: 0 > 0

  private fun convictionHasBreachOrRecallNsis(crn: String, convictionId: Long): Boolean =
    communityApiClient.getBreachRecallNsis(crn, convictionId)
      .any { NsiOutcome.from(it.status?.code) != null }

  private fun qualifyingConvictions(sentence: Sentence?): Boolean =
    sentence?.terminationDate == null ||
      sentence.terminationDate.isAfter(LocalDate.now(clock).minusYears(1).minusDays(1))

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
