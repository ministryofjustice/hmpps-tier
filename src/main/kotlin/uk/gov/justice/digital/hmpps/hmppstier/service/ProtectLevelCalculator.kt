package uk.gov.justice.digital.hmpps.hmppstier.service

import uk.gov.justice.digital.hmpps.hmppstier.domain.Registrations
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.CalculationRule.*
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa.*
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel.*
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh.*
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.RsrThresholds.*
import java.math.BigDecimal

object ProtectLevelCalculator {

  fun calculate(
    rsr: BigDecimal,
    additionalFactorsPoints: Int,
    registrations: Registrations,
  ): TierLevel<ProtectLevel> {
    val points = mapOf(
      RSR to getRsrPoints(rsr),
      ROSH to getRoshPoints(registrations.rosh),
      MAPPA to getMappaPoints(registrations.mappa),
      COMPLEXITY to getComplexityPoints(registrations.complexityFactors),
      ADDITIONAL_FACTORS_FOR_WOMEN to additionalFactorsPoints,
    )

    val total = points.map { it.value }.sum()
      .minus(minOf(points.getOrDefault(RSR, 0), points.getOrDefault(ROSH, 0)))

    return when {
      total >= LEVEL_A_LOWER_THRESHOLD -> TierLevel(A, total, points)
      total in 20 until LEVEL_A_LOWER_THRESHOLD -> TierLevel(B, total, points)
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
      VERY_HIGH -> LEVEL_A_LOWER_THRESHOLD
      HIGH -> 20
      MEDIUM -> 10
      else -> 0
    }

  private fun getMappaPoints(mappa: Mappa?): Int =
    when (mappa) {
      M3, M2 -> LEVEL_A_LOWER_THRESHOLD
      M1 -> 5
      else -> 0
    }

  private fun getComplexityPoints(complexityFactors: Collection<ComplexityFactor>): Int =
    complexityFactors.count().times(2)

  private const val LEVEL_A_LOWER_THRESHOLD = 150
}
