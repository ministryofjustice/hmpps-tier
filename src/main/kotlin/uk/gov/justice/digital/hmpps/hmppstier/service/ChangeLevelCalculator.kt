package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.AssessmentApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusAssessments
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.client.Registration
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor

@Service
class ChangeLevelCalculator(
  private val communityApiClient: CommunityApiClient,
  private val assessmentApiClient: AssessmentApiClient,
  private val assessmentApiService: AssessmentApiService
) {

  fun calculateChangeLevel(crn: String, offenderAssessment: OffenderAssessment?, deliusAssessments: DeliusAssessments?, registrations: Collection<Registration>, convictions: Collection<Conviction>): TierLevel<ChangeLevel> {
    log.info("Calculating Change Level for $crn")
    return when {
      !hasMandateForChange(crn, convictions) -> {
        log.info("No Mandate for Change for $crn")
        TierLevel(ChangeLevel.ZERO, 0)
      }
      !assessmentApiService.isAssessmentRecent(crn, offenderAssessment) -> {
        log.info("Assessment out of date for $crn")
        TierLevel(ChangeLevel.TWO, 0)
      }
      else -> {
        val totalPoints =
          getAssessmentNeedsPoints(crn, offenderAssessment) +
            getOgrsPoints(crn, deliusAssessments) +
            getIomNominalPoints(crn, registrations)
        val tier = when {
          totalPoints >= 20 -> ChangeLevel.THREE
          totalPoints in 10..19 -> ChangeLevel.TWO
          else -> ChangeLevel.ONE
        }
        TierLevel(tier, totalPoints)
      }
    }.also { log.debug("Calculated Change Level for $crn: $it") }
  }

  private fun hasMandateForChange(crn: String, convictions: Collection<Conviction>): Boolean =
    when {
      hasCurrentCustodialSentence(crn, convictions) -> true
      hasCurrentNonCustodialSentence(crn, convictions) -> hasNoUnpaidWorkOrRestrictiveRequirements(crn, convictions)
      else -> false
    }

  private fun hasCurrentCustodialSentence(crn: String, convictions: Collection<Conviction>): Boolean =
    convictions.any {
      it.sentence.sentenceType.code in custodialSentences
    }.also { log.debug("Has Current Custodial sentence for $crn: $it") }

  private fun hasCurrentNonCustodialSentence(crn: String, convictions: Collection<Conviction>): Boolean =
    convictions.any {
      it.sentence.sentenceType.code !in custodialSentences
    }.also { log.debug("Has Current Non Custodial sentence for $crn: $it") }

  private fun hasNoUnpaidWorkOrRestrictiveRequirements(crn: String, convictions: Collection<Conviction>) =
    !(hasRestrictiveRequirements(crn, convictions) || hasUnpaidWork(crn, convictions))

  fun hasRestrictiveRequirements(crn: String, convictions: Collection<Conviction>): Boolean =
    convictions.any {
      communityApiClient.getRequirements(crn, it.convictionId).any { req ->
        true == req.restrictive
      }
    }.also { log.debug("Has Restrictive Requirements for $crn: $it") }

  private fun hasUnpaidWork(crn: String, convictions: Collection<Conviction>): Boolean =
    convictions.any {
      null != it.sentence.unpaidWork?.minutesOrdered
    }.also { log.debug("Unpaid work for $crn: $it") }

  private fun getAssessmentNeedsPoints(crn: String, offenderAssessment: OffenderAssessment?): Int =
    offenderAssessment?.let { assessment ->
      assessmentApiClient.getAssessmentNeeds(assessment.assessmentId)
        .filter { it.need != null }
        .associateBy({ it.need!! }, { it.severity })
        .also {
          log.debug("Assessment Needs for $crn: $it ")
        }.let {
          it.entries.sumBy { ent ->
            ent.key.weighting.times(ent.value?.score ?: 0)
          }
        }.also { log.debug("Needs Points for $crn : $it") }
    } ?: 0

  private fun getOgrsPoints(crn: String, deliusAssessments: DeliusAssessments?): Int =
    deliusAssessments?.ogrs?.div(10).also {
      log.debug("Ogrs Points for $crn : $it")
    } ?: 0

  private fun getIomNominalPoints(crn: String, registrations: Collection<Registration>): Int =
    // We don't care about the full list, only if there is IOM Nominal
    registrations.filter { it.active }
      .mapNotNull { ComplexityFactor.from(it.type.code) }
      .let { regs ->
        if (regs.any { it == ComplexityFactor.IOM_NOMINAL }) 2 else 0
      }.also { log.debug("IOM Nominal Points for $crn : $it") }

  companion object {
    private val log = LoggerFactory.getLogger(ChangeLevelCalculator::class.java)
    private val custodialSentences = arrayOf("NC", "SC")
  }
}
