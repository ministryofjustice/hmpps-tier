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
    val points = getOasysNeedsPoints(crn).plus(getOgrsPoints(crn))
    val tier = when {
      points >= 20 -> ChangeLevel.THREE
      points in 10..19 -> ChangeLevel.TWO
      else -> ChangeLevel.ONE
    }

    return TierLevel(tier, points)
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
