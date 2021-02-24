package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusAssessments
import uk.gov.justice.digital.hmpps.hmppstier.client.Offender
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.client.Question
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

//    .filter { it.sentence.terminationDate == null }
//     .also { log.debug("Non terminated Convictions for $crn: ${it.size}") }

@Service
class ProtectLevelCalculator(
  private val clock: Clock,
  private val assessmentApiService: AssessmentApiService,
  private val communityApiClient: CommunityApiClient
) {

  fun calculateProtectLevel(
    crn: String,
    deliusOffender: Offender,
    deliusAssessments: DeliusAssessments?,
    deliusRegistrations: Collection<Registration>,
    offenderAssessmentQuestions: Collection<Question>,
    convictions: Collection<Conviction>,
    offenderAssessment: OffenderAssessment?
  ): TierLevel<ProtectLevel> {
    val riskPoints = maxOf(getRsrPoints(crn, deliusAssessments), getRoshPoints(crn, deliusRegistrations))
    val mappaPoints = getMappaPoints(crn, deliusRegistrations)

    val complexityPoints = getComplexityPoints(crn, deliusRegistrations, deliusOffender, offenderAssessmentQuestions, convictions, offenderAssessment)

    val totalPoints = riskPoints + mappaPoints + complexityPoints
    val tier = when {
      totalPoints >= 30 -> ProtectLevel.A
      totalPoints in 20..29 -> ProtectLevel.B
      totalPoints in 10..19 -> ProtectLevel.C
      else -> ProtectLevel.D
    }

    return TierLevel(tier, totalPoints)
      .also { log.debug("Calculated Protect Level for $crn: $it") }
  }

  private fun getRsrPoints(crn: String, deliusAssessments: DeliusAssessments?): Int =
    deliusAssessments?.rsr?.let {
      when {
        it >= RsrThresholds.TIER_B_RSR.num -> 20
        it >= RsrThresholds.TIER_C_RSR.num -> 10
        else -> 0
      }
    } ?: 0
      .also { log.debug("RSR Points for $crn : $it") }

  private fun getRoshPoints(crn: String, deliusRegistrations: Collection<Registration>): Int =
    deliusRegistrations
      .mapNotNull { Rosh.from(it.type.code) }
      .firstOrNull()
      .let { rosh ->
        when (rosh) {
          Rosh.VERY_HIGH -> 30
          Rosh.HIGH -> 20
          Rosh.MEDIUM -> 10
          else -> 0
        }.also { log.debug("ROSH Points for $crn: $it from value $rosh") }
      }

  private fun getMappaPoints(crn: String, deliusRegistrations: Collection<Registration>): Int =
    deliusRegistrations
      .mapNotNull { reg -> Mappa.from(reg.registerLevel?.code) }
      .firstOrNull()
      .let { mappa ->
        when (mappa) {
          Mappa.M3, Mappa.M2 -> 30
          Mappa.M1 -> 5
          else -> 0
        }.also { log.debug("MAPPA Points for $crn : $it from value $mappa") }
      }

  private fun getComplexityPoints(crn: String, deliusRegistrations: Collection<Registration>, offender: Offender, assessmentQuestions: Collection<Question>, convictions: Collection<Conviction>, offenderAssessment: OffenderAssessment?): Int =
    deliusRegistrations
      .mapNotNull { ComplexityFactor.from(it.type.code) }
      .distinct()
      .filter { it != ComplexityFactor.IOM_NOMINAL }
      .count()
      .also { log.debug("Complexity factor size for $crn : $it") }
      .let { regCount ->
        when {
          offender.gender.equals("female", true) -> {
            log.debug("$crn is Female")
            regCount + getAssessmentComplexityPoints(crn, assessmentQuestions, offenderAssessment) + getBreachRecallComplexityPoints(crn, convictions)
          }
          else -> regCount
        }.times(2)
      }.also { log.debug("Complexity Points for $crn : $it") }

  private fun getAssessmentComplexityPoints(crn: String, assessmentQuestions: Collection<Question>, offenderAssessment: OffenderAssessment?): Int =
    if (assessmentApiService.isAssessmentRecent(crn, offenderAssessment)) {
      assessmentQuestions
        // todo: is needed?  .filter { AssessmentComplexityFactor.from(it.questionCode) != null }
        .associateBy({ AssessmentComplexityFactor.from(it.questionCode) }, { it.answers.firstOrNull()?.refAnswerCode })
        .also { log.debug("Assessment Complexity answers for $crn: $it ") }
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
          parenting.plus(selfControl)
        }.also { log.debug("Assessment Complexity Points for $crn : $it") }
    } else 0

  private fun getBreachRecallComplexityPoints(crn: String, convictions: Collection<Conviction>): Int =
    convictions.filter {
      it.sentence.terminationDate == null ||
        it.sentence.terminationDate!!.isAfter(LocalDate.now(clock).minusYears(1).minusDays(1))
    }.also { log.debug("Breach Qualifying Convictions for $crn: ${it.size}") }
      .let {
        if (it.any { conviction -> convictionHasBreachOrRecallNsis(crn, conviction) }) 1
        else 0
      }.also { log.debug("Breach and Recall Complexity Points for $crn : $it") }

  private fun convictionHasBreachOrRecallNsis(crn: String, conviction: Conviction): Boolean =
    communityApiClient.getBreachRecallNsis(crn, conviction.convictionId)
      .any { NsiStatus.from(it.status.code) != null }

  private fun isYes(value: String?): Boolean =
    value.equals("YES", true) || value.equals("Y", true)

  private fun isAnswered(value: String?): Boolean =
    value?.toInt() ?: 0 > 0

  companion object {
    private val log = LoggerFactory.getLogger(ProtectLevelCalculator::class.java)
  }
}
