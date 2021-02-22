package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AssessmentComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.RsrThresholds

@Service
class ProtectLevelCalculator(
  private val communityApiDataService: CommunityApiDataService,
  private val assessmentApiDataService: AssessmentApiDataService
) {

  fun calculateProtectLevel(crn: String): TierLevel<ProtectLevel> {
    val riskPoints = maxOf(getRsrPoints(crn), getRoshPoints(crn))
    val mappaPoints = getMappaPoints(crn)
    val complexityPoints = getComplexityPoints(crn)

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

  private fun getRsrPoints(crn: String): Int =
    communityApiDataService.getRSR(crn)?.let {
      when {
        it >= RsrThresholds.TIER_B_RSR.num -> 20
        it >= RsrThresholds.TIER_C_RSR.num -> 10
        else -> 0
      }
    } ?: 0
      .also { log.debug("RSR Points for $crn : $it") }

  private fun getRoshPoints(crn: String): Int =
    when (communityApiDataService.getRosh(crn)) {
      Rosh.VERY_HIGH -> 30
      Rosh.HIGH -> 20
      Rosh.MEDIUM -> 10
      else -> 0
    }.also { log.debug("ROSH Points for $crn : $it") }

  private fun getMappaPoints(crn: String): Int =
    when (communityApiDataService.getMappa(crn)) {
      Mappa.M3, Mappa.M2 -> 30
      Mappa.M1 -> 5
      else -> 0
    }.also { log.debug("MAPPA Points for $crn : $it") }

  private fun getComplexityPoints(crn: String): Int =
    getRelevantComplexityFactors(crn).count().times(2).let {
      when {
        communityApiDataService.isFemaleOffender(crn) -> {
          it + getAssessmentComplexityPoints(crn) + getBreachRecallComplexityPoints(crn)
        }
        else -> it
      }
    }.also { log.debug("Complexity Points for $crn : $it") }

  private fun getRelevantComplexityFactors(crn: String): Collection<ComplexityFactor> =
    communityApiDataService.getComplexityFactors(crn).distinct().filter {
      // Filter out IOM it is not used in this calculation but is used in the change level calculation
      it != ComplexityFactor.IOM_NOMINAL
    }.also { log.debug("Relevant Complexity factor size for $crn : ${it.size}") }

  private fun getAssessmentComplexityPoints(crn: String): Int =
    assessmentApiDataService.getAssessmentComplexityAnswers(crn).let {
      val parenting = when {
        isYes(it[AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES]) -> 2
        else -> 0
      }
      // We dont take the cumulative score, just '2' if at least one of these two is present
      val selfControl = when {
        isAnswered(it[AssessmentComplexityFactor.IMPULSIVITY]) || isAnswered(it[AssessmentComplexityFactor.TEMPER_CONTROL]) -> 2
        else -> 0
      }
      parenting.plus(selfControl)
    }.also { log.debug("Assessment Complexity Points for $crn : $it") }

  private fun getBreachRecallComplexityPoints(crn: String): Int =
    when {
      communityApiDataService.hasBreachedConvictions(crn) -> 2
      else -> 0
    }.also { log.debug("Breach and Recall Complexity Points for $crn : $it") }

  private fun isYes(value: String?): Boolean =
    value != null && (value.equals("YES", true) || value.equals("Y", true))

  private fun isAnswered(value: String?): Boolean =
    value != null && value.toInt() > 0

  companion object {
    private val log = LoggerFactory.getLogger(ProtectLevelCalculator::class.java)
  }
}
