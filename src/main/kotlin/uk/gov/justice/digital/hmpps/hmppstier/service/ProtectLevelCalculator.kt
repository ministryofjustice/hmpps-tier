package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.CalculationRule
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.CalculationRule.ADDITIONAL_FACTORS_FOR_WOMEN
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.CalculationRule.COMPLEXITY
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.CalculationRule.MAPPA
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.CalculationRule.ROSH
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.CalculationRule.RSR
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa.M1
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa.M2
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa.M3
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel.A
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel.B
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel.C
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel.D
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh.HIGH
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh.MEDIUM
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh.VERY_HIGH
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.RsrThresholds.TIER_B_RSR_LOWER
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.RsrThresholds.TIER_B_RSR_UPPER
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.RsrThresholds.TIER_C_RSR_LOWER
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.RsrThresholds.TIER_C_RSR_UPPER
import java.math.BigDecimal

@Service
class ProtectLevelCalculator(
  private val additionalFactorsForWomen: AdditionalFactorsForWomen,
  private val calcVer: CalculationVersionHelper
) {

  fun calculateProtectLevel(
    crn: String,
    offenderAssessment: OffenderAssessment?,
    rsr: BigDecimal,
    rosh: Rosh?,
    mappa: Mappa?,
    complexityFactors: Collection<ComplexityFactor>,
    convictions: Collection<Conviction>
  ): TierLevel<ProtectLevel> {

    val points = mapOf(
      RSR to getRsrPoints(rsr),
      ROSH to getRoshPoints(rosh),
      MAPPA to getMappaPoints(mappa),
      COMPLEXITY to getComplexityPoints(complexityFactors),
      ADDITIONAL_FACTORS_FOR_WOMEN to additionalFactorsForWomen.calculate(crn, convictions, offenderAssessment)
    )

    val total = points.map { it.value }.sum()
      .minus(minOf(points.getOrDefault(RSR, 0), points.getOrDefault(ROSH, 0)))

    return TierLevel(
      when {
        total >= 30 && hasHighMappaOrVeryHighRosh(points) -> A
        total >= 20 -> B
        total in 10 until 20 -> C
        else -> D
      },
      total,
      points
    )
  }

  private fun hasHighMappaOrVeryHighRosh(points: Map<CalculationRule, Int>): Boolean =
    !calcVer.tierAThresholdFixEnabled() ||
      when {
        points.getOrDefault(MAPPA, 0) >= getMappaPoints(M2) -> true
        points.getOrDefault(MAPPA, 0) >= getMappaPoints(M3) -> true
        points.getOrDefault(ROSH, 0) >= getRoshPoints(VERY_HIGH) -> true
        else -> false
      }

  private fun getRsrPoints(rsr: BigDecimal): Int =
    when (rsr) {
      in TIER_B_RSR_LOWER.num..TIER_B_RSR_UPPER.num -> 20
      in TIER_C_RSR_LOWER.num..TIER_C_RSR_UPPER.num -> 10
      else -> 0
    }

  private fun getRoshPoints(rosh: Rosh?): Int =
    when (rosh) {
      VERY_HIGH -> 30
      HIGH -> 20
      MEDIUM -> 10
      else -> 0
    }

  private fun getMappaPoints(mappa: Mappa?): Int =
    when (mappa) {
      M3, M2 -> 30
      M1 -> 5
      else -> 0
    }

  private fun getComplexityPoints(complexityFactors: Collection<ComplexityFactor>): Int =
    complexityFactors.count().times(2)
}
