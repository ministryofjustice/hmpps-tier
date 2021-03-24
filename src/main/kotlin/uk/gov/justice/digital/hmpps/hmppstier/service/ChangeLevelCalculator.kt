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
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel.ONE
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel.THREE
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel.TWO
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel.ZERO
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

    val orderedRegistrations = deliusRegistrations
      .filter { it.active }
      .sortedByDescending { it.startDate }

    return when {
      !hasMandateForChange(crn, convictions) -> {
        TierLevel(ZERO, 0)
      }
      offenderAssessment == null -> {
        log.info("No valid assessment found for $crn")
        TierLevel(TWO, 0)
      }
      else -> {
        val needsPoints = getAssessmentNeedsPoints(offenderAssessment)
        val ogrsPoints = getOgrsPoints(deliusAssessments)
        val iomPoints = getIomNominalPoints(orderedRegistrations)

        val totalPoints = needsPoints + ogrsPoints + iomPoints

        when {
          totalPoints >= 20 -> TierLevel(THREE, totalPoints)
          totalPoints in 10..19 -> TierLevel(TWO, totalPoints)
          else -> TierLevel(ONE, totalPoints)
        }
      }
    }.also { log.debug("Calculated Change Level for $crn: $it") }
  }

  private fun hasMandateForChange(crn: String, convictions: Collection<Conviction>): Boolean =
    convictions
      .filter { it.sentence?.terminationDate == null }
      .let { activeConvictions ->
        activeConvictions.any {
          it.sentence != null && (isCustodialSentence(it.sentence) || hasNonRestrictiveRequirements(crn, it.convictionId))
        }
      }.also { log.debug("Has Mandate for change: $it") }

  private fun isCustodialSentence(sentence: Sentence) =
    sentence.sentenceType.code in custodialSentences

  private fun hasNonRestrictiveRequirements(crn: String, convictionId: Long): Boolean =
    communityApiClient.getRequirements(crn, convictionId)
      .any { req ->
        req.restrictive != true
      }.also { log.debug("Has non-restrictive requirements: $it") }

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
