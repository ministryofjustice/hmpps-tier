package uk.gov.justice.digital.hmpps.hmppstier.domain

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AssessmentComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeScore
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectScore
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.RsrThresholds
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.TierMatchCriteria
import java.math.BigDecimal

@Component
class TierCalculation {

  private val protectRules = ProtectRules()
  private val changeRules = ChangeRules()

  fun calculateTier(protectScores: ProtectScores, changeScores: ChangeScores): TierCalculationResult {
    return TierCalculationResult(
      protectScore = calculateProtectScore(protectScores),
      changeScore = calculateChangeScore(changeScores)
    )
  }

  private fun calculateProtectScore(protectScores: ProtectScores): TierResult<ProtectScore> {
    protectRules.assessAgainstRiskScores(protectScores).let {
      log.debug("Matched rule for CRN: ${protectScores.crn}, Tier: ${it.tier}, Score: ${it.score}, Matching Criteria: ${it.criteria}")
      return it
    }
  }

  private fun calculateChangeScore(changeScores: ChangeScores): TierResult<ChangeScore> {
    changeRules.assessAgainstRiskScores(changeScores).let {
      log.debug("Matched rule for CRN: ${changeScores.crn}, Tier: ${it.tier}, Score: ${it.score}, Matching Criteria: ${it.criteria}")
      return it
    }
  }

  internal class ProtectRules {
    private val criteria: MutableSet<TierMatchCriteria> = mutableSetOf()

    fun assessAgainstRiskScores(riskScores: ProtectScores): TierResult<ProtectScore> {

      val riskPoints = getRiskPoints(riskScores)
      val mappaPoints = getMappaPoints(riskScores.mappaLevel)
      val complexityPoints = getComplexityPoints(riskScores.complexityFactors, riskScores.assessmentComplexityFactors)

      val score = riskPoints + mappaPoints + complexityPoints
      val tier = when {
        score >= 30 -> {
          ProtectScore.A
        }
        score in 20..29 -> {
          ProtectScore.B
        }
        score in 10..19 -> {
          ProtectScore.C
        }
        else -> {
          ProtectScore.D
        }
      }

      return TierResult(tier, score, criteria)
    }

    private fun getRiskPoints(riskScores: ProtectScores): Int {
      val rsrPoints = getRsrPoints(riskScores.rsrScore)
      val roshPoints = getRoshPoints(riskScores.roshScore)
      return when {
        rsrPoints > roshPoints -> {
          criteria.add(TierMatchCriteria.RSR_USED_OVER_ROSH)
          rsrPoints
        }
        roshPoints > rsrPoints -> {
          criteria.add(TierMatchCriteria.ROSH_USED_OVER_RSR)
          roshPoints
        }
        else -> {
          criteria.add(TierMatchCriteria.RSR_ROSH_EQUAL)
          roshPoints
        }
      }
    }

    private fun getRsrPoints(rsrScore: BigDecimal?): Int {
      return when {
        rsrScore != null && rsrScore >= RsrThresholds.TIER_B_RSR.num -> {
          criteria.add(TierMatchCriteria.RSR_IN_TIER_B)
          20
        }
        rsrScore != null && rsrScore >= RsrThresholds.TIER_C_RSR.num -> {
          criteria.add(TierMatchCriteria.RSR_IN_TIER_C)
          10
        }
        else -> {
          criteria.add(TierMatchCriteria.RSR_NO_MATCH)
          0
        }
      }
    }

    private fun getRoshPoints(roshScore: Rosh?): Int {
      return when (roshScore) {
        Rosh.VERY_HIGH -> {
          criteria.add(TierMatchCriteria.ROSH_VERY_HIGH)
          30
        }
        Rosh.HIGH -> {
          criteria.add(TierMatchCriteria.ROSH_HIGH)
          20
        }
        Rosh.MEDIUM -> {
          criteria.add(TierMatchCriteria.ROSH_MEDIUM)
          10
        }
        else -> {
          criteria.add(TierMatchCriteria.ROSH_NO_MATCH)
          0
        }
      }
    }

    private fun getMappaPoints(mappaLevel: Mappa?): Int {
      return when (mappaLevel) {
        Mappa.M2, Mappa.M3 -> {
          criteria.add(TierMatchCriteria.MAPPA_LEVEL_2_OR_3)
          30
        }
        Mappa.M1 -> {
          criteria.add(TierMatchCriteria.MAPPA_LEVEL_1)
          5
        }
        else -> {
          criteria.add(TierMatchCriteria.MAPPA_NO_MATCH)
          0
        }
      }
    }

    private fun getComplexityPoints(
      complexityFactors: List<ComplexityFactor>,
      assessmentComplexityFactors: Map<AssessmentComplexityFactor, String?>
    ): Int {

      return if (complexityFactors.any() || assessmentComplexityFactors.any()) {
        criteria.add(TierMatchCriteria.INCLUDED_COMPLEXITY_FACTORS)
        (complexityFactors.distinct().count().plus(getAssessmentComplexityPoints(assessmentComplexityFactors))) * 2
      } else {
        criteria.add(TierMatchCriteria.NO_COMPLEXITY_FACTORS)
        0
      }
    }

    private fun getAssessmentComplexityPoints(factors : Map<AssessmentComplexityFactor, String?>): Int {

      val parentingAnswer = factors.getOrDefault(AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES, "N")
      val parenting = if (isYes(parentingAnswer)) 1 else 0

      // We dont take the cumulative score, just '1' if at least one of these two is present
      val impulsivityAnswer = factors[AssessmentComplexityFactor.IMPULSIVITY]?.toInt() ?: 0
      val temperControlAnswer = factors.getOrDefault(AssessmentComplexityFactor.TEMPER_CONTROL, "0")?.toInt() ?: 0
      val selfControl = if (impulsivityAnswer.plus(temperControlAnswer) > 1) 1 else 0

      return parenting.plus(selfControl)
    }

    private fun isYes(value: String?): Boolean {
      return "YES".equals(value, true) || "Y".equals(value, true)
    }

  }


  internal class ChangeRules {
    private val criteria: MutableSet<TierMatchCriteria> = mutableSetOf()

    fun assessAgainstRiskScores(riskScores: ChangeScores): TierResult<ChangeScore> {
      val ogrsPoints = getOgrsPoints(riskScores.ogrsScore)
      val oasysPoints = getOasysNeedsPoints(riskScores.need)

      val score = ogrsPoints + oasysPoints
      val tier = when {
        score >= 20 -> {
          ChangeScore.THREE
        }
        score in 10..19 -> {
          ChangeScore.TWO
        }
        else -> {
          ChangeScore.ONE
        }
      }

      return TierResult(tier, score, criteria)
    }

    private fun getOgrsPoints(ogrsScore: Int?): Int {
      return if (ogrsScore != null) {
        criteria.add(TierMatchCriteria.INCLUDED_ORGS)
        ogrsScore / 10
      } else {
        criteria.add(TierMatchCriteria.NO_ORGS)
        0
      }
    }

    private fun getOasysNeedsPoints(need: Map<Need, NeedSeverity?>): Int {
      return if (need.any()) {
        criteria.add(TierMatchCriteria.INCLUDED_OASYS_NEEDS)
        need.entries.sumBy {
          it.key.weighting * (it.value?.score ?: 0)
        }
      } else {
        criteria.add(TierMatchCriteria.NO_OASYS_NEEDS)
        0
      }
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(TierCalculation::class.java)
  }
}
