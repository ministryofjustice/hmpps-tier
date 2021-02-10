package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel

@Service
class ChangeLevelCalculator(
  private val communityApiDataService: CommunityApiDataService,
  private val assessmentApiDataService: AssessmentApiDataService
) {

  fun calculateChangeLevel(crn: String): TierLevel<ChangeLevel> {
    return if (hasMandateForChange(crn)) {
      when {
          !assessmentApiDataService.isLatestAssessmentRecent(crn) -> {
            TierLevel(ChangeLevel.TWO, 0)
          }
          else -> {
            calculateTier(getAssessmentNeedsPoints(crn).plus(getOgrsPoints(crn)))
          }
      }
    } else TierLevel(ChangeLevel.ZERO, 0)
  }

  private fun hasMandateForChange(crn: String): Boolean =
    when {
      communityApiDataService.isCurrentCustodialSentence(crn) -> true
      communityApiDataService.isCurrentNonCustodialSentence(crn) -> hasNoUnpaidWorkOrRestrictiveRequirements(crn)
      else -> false
    }

  private fun hasNoUnpaidWorkOrRestrictiveRequirements(crn: String) =
    !(communityApiDataService.hasRestrictiveRequirements(crn) || communityApiDataService.hasUnpaidWork(crn))

  private fun calculateTier(points: Int): TierLevel<ChangeLevel> = when {
    points >= 20 -> TierLevel(ChangeLevel.THREE, points)
    points in 10..19 -> TierLevel(ChangeLevel.TWO, points)
    else -> TierLevel(ChangeLevel.ONE, points)
  }

  private fun getAssessmentNeedsPoints(crn: String): Int =
    assessmentApiDataService.getAssessmentNeeds(crn).let {
      it.entries.sumBy { ent ->
        ent.key.weighting.times(ent.value?.score ?: 0)
      }
    }

  private fun getOgrsPoints(crn: String): Int =
    communityApiDataService.getOGRS(crn).let {
      it?.div(10) ?: 0
    }

}
