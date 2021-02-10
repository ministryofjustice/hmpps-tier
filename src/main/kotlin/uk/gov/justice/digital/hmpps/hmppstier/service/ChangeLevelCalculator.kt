package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor

@Service
class ChangeLevelCalculator(
  private val communityApiDataService: CommunityApiDataService,
  private val assessmentApiDataService: AssessmentApiDataService
) {

  fun calculateChangeLevel(crn: String): TierLevel<ChangeLevel> {
    if (shouldCalculateChangeLevel(crn)) {

      val points = getOasysNeedsPoints(crn) + getOgrsPoints(crn) + getIomNominal(crn)
      val tier = calculateTier(points)

      return TierLevel(tier, points)
    }
    return TierLevel(ChangeLevel.ZERO, 0)
  }

  private fun getIomNominal(crn: String): Int {
    // We don't care about the full list, only if there is IOM Nominal
    return if (communityApiDataService.getComplexityFactors(crn).any { it == ComplexityFactor.IOM_NOMINAL }) 2 else 0
  }

  private fun shouldCalculateChangeLevel(crn: String): Boolean =
    when {
      communityApiDataService.isCurrentCustodialSentence(crn) -> true
      communityApiDataService.isCurrentNonCustodialSentence(crn) -> hasNoUnpaidWorkOrRestrictiveRequirements(crn)
      else -> false
    }

  private fun hasNoUnpaidWorkOrRestrictiveRequirements(crn: String) =
    !(communityApiDataService.hasRestrictiveRequirements(crn) || communityApiDataService.hasUnpaidWork(crn))

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
