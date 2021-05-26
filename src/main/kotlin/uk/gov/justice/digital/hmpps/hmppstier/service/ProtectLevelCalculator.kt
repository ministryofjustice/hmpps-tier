package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
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

    return when {
      total >= levelA() -> TierLevel(A, total, points)
      total in 20 until levelA() -> TierLevel(B, total, points)
      total in 10 until 20 -> TierLevel(C, total, points)
      else -> TierLevel(D, total, points)
    }
  }

  private fun getRsrPoints(rsr: BigDecimal): Int =
    when (rsr) {
      in TIER_B_RSR_LOWER.num..TIER_B_RSR_UPPER.num -> 20
      in TIER_C_RSR_LOWER.num..TIER_C_RSR_UPPER.num -> 10
      else -> 0
    }

  private fun getRoshPoints(rosh: Rosh?): Int =
    when (rosh) {
      VERY_HIGH -> levelA()
      HIGH -> 20
      MEDIUM -> 10
      else -> 0
    }

  private fun getMappaPoints(mappa: Mappa?): Int =
    when (mappa) {
      M3, M2 -> levelA()
      M1 -> 5
      else -> 0
    }

  private fun levelA() = if (calcVer.enableTierAFix()) 150 else 30

  private fun getComplexityPoints(complexityFactors: Collection<ComplexityFactor>): Int =
    complexityFactors.count().times(2)
}
