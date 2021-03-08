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
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor

@Service
class ChangeLevelCalculator(
  private val communityApiClient: CommunityApiClient,
  private val assessmentApiService: AssessmentApiService,
) {

  fun calculateChangeLevel(
    crn: String,
    offenderAssessment: OffenderAssessment?,
    deliusAssessments: DeliusAssessments?,
    deliusRegistrations: Collection<Registration>,
    convictions: Collection<Conviction>
  ): TierLevel<ChangeLevel> {
    log.info("Calculating Change Level for $crn")

    val orderedRegistrations = deliusRegistrations
      .filter { it.active }
      .sortedByDescending { it.startDate }

    return when {
      !hasMandateForChange(crn, convictions) -> {
        log.info("No Mandate for Change for $crn")
        TierLevel(ChangeLevel.ZERO, 0)
      }
      offenderAssessment == null -> {
        log.info("Assessment out of date for $crn")
        TierLevel(ChangeLevel.TWO, 0)
      }
      else -> {
        val needsPoints = getAssessmentNeedsPoints(offenderAssessment)
        val ogrsPoints = getOgrsPoints(deliusAssessments)
        val iomPoints = getIomNominalPoints(orderedRegistrations)

        val totalPoints = needsPoints + ogrsPoints + iomPoints

        when {
          totalPoints >= 20 -> TierLevel(ChangeLevel.THREE, totalPoints)
          totalPoints in 10..19 -> TierLevel(ChangeLevel.TWO, totalPoints)
          else -> TierLevel(ChangeLevel.ONE, totalPoints)
        }
      }
    }.also { log.debug("Calculated Change Level for $crn: $it") }
  }

  private fun hasMandateForChange(crn: String, convictions: Collection<Conviction>): Boolean =
    convictions
      .filter { it.sentence?.terminationDate == null }
      .let { activeConvictions ->
        activeConvictions.any {
          it.sentence?.sentenceType?.code in custodialSentences ||
            !(hasRestrictiveRequirements(crn, it.convictionId) || hasUnpaidWork(it.sentence))
        }
      }.also { log.debug("Has Mandate for change: $it") }

  //only used from hasMandateForChange so can be changed
  private fun hasRestrictiveRequirements(crn: String, convictionId: Long): Boolean =
    communityApiClient.getRequirements(crn, convictionId)
      .any { req ->
        req.restrictive ?: false
      }.also { log.debug("Has Restrictive Requirements: $it") }

  private fun hasUnpaidWork(sentence: Sentence?): Boolean =
    (sentence?.unpaidWork?.minutesOrdered != null)
      .also { log.debug("Unpaid work $it") }

  private fun getAssessmentNeedsPoints(offenderAssessment: OffenderAssessment?): Int =
    (
      offenderAssessment?.let { assessment ->
        assessmentApiService.getAssessmentNeeds(assessment.assessmentId)
          .also {
            log.debug("Assessment Needs: $it ")
          }.let {
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
    // We don't care about the full list, only if there is IOM Nominal
    when {
      registrations.any { ComplexityFactor.from(it.type.code) == ComplexityFactor.IOM_NOMINAL } -> 2
      else -> 0
    }.also { log.debug("IOM Nominal Points: $it") }

  companion object {
    private val log = LoggerFactory.getLogger(ChangeLevelCalculator::class.java)
    private val custodialSentences = arrayOf("NC", "SC")
  }
}
