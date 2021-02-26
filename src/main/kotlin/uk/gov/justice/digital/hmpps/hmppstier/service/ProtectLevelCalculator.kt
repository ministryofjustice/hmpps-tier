package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusAssessments
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.client.Registration
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AssessmentComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NsiStatus
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
    deliusRegistrations: Collection<Registration>,
    convictions: Collection<Conviction>
  ): TierLevel<ProtectLevel> {

    val rsrPoints = getRsrPoints(deliusAssessments)
    val roshPoints = getRoshPoints(deliusRegistrations)
    val mappaPoints = getMappaPoints(deliusRegistrations)
    val complexityPoints = getComplexityPoints(deliusRegistrations)
    val femaleComplexityPoints = getFemaleOnlyComplexityPoints(crn, convictions, offenderAssessment)

    val totalPoints = maxOf(rsrPoints, roshPoints) + mappaPoints + (complexityPoints + femaleComplexityPoints).times(2)

    return when {
      totalPoints >= 30 -> TierLevel(ProtectLevel.A, totalPoints)
      totalPoints in 20..29 -> TierLevel(ProtectLevel.B, totalPoints)
      totalPoints in 10..19 -> TierLevel(ProtectLevel.C, totalPoints)
      else -> TierLevel(ProtectLevel.D, totalPoints)
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

  private fun getRoshPoints(deliusRegistrations: Collection<Registration>): Int =
    deliusRegistrations
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

  private fun getMappaPoints(deliusRegistrations: Collection<Registration>): Int =
    deliusRegistrations
      .mapNotNull { Mappa.from(it.registerLevel?.code) }
      .firstOrNull()
      .let { mappa ->
        when (mappa) {
          Mappa.M3, Mappa.M2 -> 30
          Mappa.M1 -> 5
          else -> 0
        }
      }.also { log.debug("MAPPA Points: $it") }

  private fun getComplexityPoints(deliusRegistrations: Collection<Registration>): Int =
    deliusRegistrations
      .mapNotNull { ComplexityFactor.from(it.type.code) }
      .distinct()
      .filter { it != ComplexityFactor.IOM_NOMINAL }
      .count()
      .also { log.debug("Complexity factor size: $it") }

  private fun getFemaleOnlyComplexityPoints(
    crn: String,
    convictions: Collection<Conviction>,
    offenderAssessment: OffenderAssessment?
  ): Int =
    when {
      communityApiClient.getOffender(crn).gender.equals("female", true) -> {
        getAssessmentComplexityPoints(offenderAssessment) + getBreachRecallComplexityPoints(crn, convictions)
      }
      else -> 0
    }.also { log.debug("Complexity Points for $crn : $it") }

  private fun getAssessmentComplexityPoints(offenderAssessment: OffenderAssessment?): Int =
    when (offenderAssessment) {
      null -> 0
      else -> {
        assessmentApiService.getAssessmentAnswers(offenderAssessment.assessmentId)
          .also { log.debug("Assessment Complexity answers $it ") }
          .let { answers ->
            val parenting = when {
              isYes(answers[AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES]) -> 1
              else -> 0
            }
            // We dont take the cumulative score, just '1' if at least one of these two is present
            val selfControl = when {
              isAnswered(answers[AssessmentComplexityFactor.IMPULSIVITY]) || isAnswered(answers[AssessmentComplexityFactor.TEMPER_CONTROL]) -> 1
              else -> 0
            }
            parenting + selfControl
          }
      }
    }.also { log.debug("Assessment Complexity Points $it") }

  private fun getBreachRecallComplexityPoints(crn: String, convictions: Collection<Conviction>): Int =
    convictions
      .filter { qualifyingConvictions(it) }
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
      .any { NsiStatus.from(it.status.code) != null }

  private fun qualifyingConvictions(conviction: Conviction): Boolean =
    conviction.sentence.terminationDate == null ||
      conviction.sentence.terminationDate!!.isAfter(LocalDate.now(clock).minusWeeks(52).minusDays(1))

  companion object {
    private val log = LoggerFactory.getLogger(ProtectLevelCalculator::class.java)
  }
}
