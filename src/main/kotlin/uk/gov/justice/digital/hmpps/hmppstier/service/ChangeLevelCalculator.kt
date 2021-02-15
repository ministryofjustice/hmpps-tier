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
    return if (hasMandateForChange(crn)) {
      val totalPoints = getOasysNeedsPoints(crn) + getOgrsPoints(crn) + getIomNominalPoints(crn)
      val tier = when {
        totalPoints >= 20 -> ChangeLevel.THREE
        totalPoints in 10..19 -> ChangeLevel.TWO
        else -> ChangeLevel.ONE
      }
      TierLevel(tier, totalPoints)
    } else TierLevel(ChangeLevel.ZERO, 0)
  }

  private fun hasMandateForChange(crn: String): Boolean =
    when {
      communityApiDataService.hasCurrentCustodialSentence(crn) -> true
      communityApiDataService.hasCurrentNonCustodialSentence(crn) -> hasNoUnpaidWorkOrRestrictiveRequirements(crn)
      else -> false
    }

  private fun hasNoUnpaidWorkOrRestrictiveRequirements(crn: String) =
    !(communityApiDataService.hasRestrictiveRequirements(crn) || communityApiDataService.hasUnpaidWork(crn))

  private fun getOasysNeedsPoints(crn: String): Int =
    assessmentApiDataService.getAssessmentNeeds(crn).let {
      it.entries.sumBy { ent ->
        ent.key.weighting.times(ent.value?.score ?: 0)
      }
    }

  private fun getOgrsPoints(crn: String): Int =
    communityApiDataService.getOGRS(crn).let {
      it?.div(10) ?: 0
    }

  private fun getIomNominalPoints(crn: String): Int =
    // We don't care about the full list, only if there is IOM Nominal
    if (communityApiDataService.getComplexityFactors(crn).any { it == ComplexityFactor.IOM_NOMINAL }) 2 else 0
}
