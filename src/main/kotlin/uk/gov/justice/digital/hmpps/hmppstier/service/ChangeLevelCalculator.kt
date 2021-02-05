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
    if (shouldCalculateChangeLevel(crn)) {

      val points = getOasysNeedsPoints(crn).plus(getOgrsPoints(crn))
      val tier = calculateTier(points)

      return TierLevel(tier, points)
    }
    return TierLevel(ChangeLevel.ZERO, 0)
  }

  private fun shouldCalculateChangeLevel(crn: String): Boolean {
    val isCurrentCustodial = communityApiDataService.isCurrentCustodialSentence(crn)
    if (isCurrentCustodial) {
      return true
    }
    return nonCustodialWithNoRestrictiveRequirementsOrUnpaidWork(crn)
  }

  private fun nonCustodialWithNoRestrictiveRequirementsOrUnpaidWork(crn: String) =
    communityApiDataService.isCurrentNonCustodialSentence(crn) && !(communityApiDataService.hasRestrictiveRequirements(crn) || communityApiDataService.hasUnpaidWork(crn))

  private fun calculateTier(points: Int) = when {
    points >= 20 -> ChangeLevel.THREE
    points in 10..19 -> ChangeLevel.TWO
    else -> ChangeLevel.ONE
  }

  private fun getOasysNeedsPoints(crn: String): Int {
    return assessmentApiDataService.getAssessmentNeeds(crn).let {
      it.entries.sumBy { ent ->
        ent.key.weighting.times(ent.value?.score ?: 0)
      }
    }
  }

  private fun getOgrsPoints(crn: String): Int {
    return communityApiDataService.getOGRS(crn).let {
      it?.div(10) ?: 0
    }
  }
}
