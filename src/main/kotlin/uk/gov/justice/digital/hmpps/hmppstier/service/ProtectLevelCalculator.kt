package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusAssessments
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.client.Registration
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.CalculationRule
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.RsrThresholds.TIER_B_RSR_LOWER
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.RsrThresholds.TIER_B_RSR_UPPER
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.RsrThresholds.TIER_C_RSR_LOWER
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.RsrThresholds.TIER_C_RSR_UPPER

@Service
class ProtectLevelCalculator(
  private val additionalFactorsForWomen: AdditionalFactorsForWomen
) {

  fun calculateProtectLevel(
    crn: String,
    offenderAssessment: OffenderAssessment?,
    deliusAssessments: DeliusAssessments?,
    registrations: Collection<Registration>,
    convictions: Collection<Conviction>
  ): TierLevel<ProtectLevel> {

    val points = mapOf(
      CalculationRule.RSR to getRsrPoints(deliusAssessments),
      CalculationRule.ROSH to getRoshPoints(registrations),
      CalculationRule.MAPPA to getMappaPoints(registrations),
      CalculationRule.COMPLEXITY to getComplexityPoints(registrations),
      CalculationRule.ADDITIONAL_FACTORS_FOR_WOMEN to additionalFactorsForWomen.getAdditionalFactorsForWomen(crn, convictions, offenderAssessment)
    )

    val total = points.map { it.value }.sum()
      .minus(minOf(points.getOrDefault(CalculationRule.RSR, 0), points.getOrDefault(CalculationRule.ROSH, 0)))

    return when {
      total >= 30 -> TierLevel(ProtectLevel.A, total, points)
      total in 20..29 -> TierLevel(ProtectLevel.B, total, points)
      total in 10..19 -> TierLevel(ProtectLevel.C, total, points)
      else -> TierLevel(ProtectLevel.D, total, points)
    }
  }

  private fun getRsrPoints(deliusAssessments: DeliusAssessments?): Int =
    deliusAssessments?.rsr
      ?.let { rsr ->
        when (rsr) {
          in TIER_B_RSR_LOWER.num..TIER_B_RSR_UPPER.num -> 20
          in TIER_C_RSR_LOWER.num..TIER_C_RSR_UPPER.num -> 10
          else -> 0
        }
      } ?: 0

  private fun getRoshPoints(registrations: Collection<Registration>): Int =
    registrations
      .mapNotNull { Rosh.from(it.type.code) }
      .firstOrNull()
      .let { rosh ->
        when (rosh) {
          Rosh.VERY_HIGH -> 30
          Rosh.HIGH -> 20
          Rosh.MEDIUM -> 10
          else -> 0
        }
      }

  private fun getMappaPoints(registrations: Collection<Registration>): Int =
    registrations
      .mapNotNull { Mappa.from(it.registerLevel?.code) }
      .firstOrNull()
      .let { mappa ->
        when (mappa) {
          Mappa.M3, Mappa.M2 -> 30
          Mappa.M1 -> 5
          else -> 0
        }
      }

  private fun getComplexityPoints(registrations: Collection<Registration>): Int =
    registrations
      .mapNotNull { ComplexityFactor.from(it.type.code) }
      .distinct()
      .count()
      .times(2)
}
