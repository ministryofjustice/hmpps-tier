package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusAssessments
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.client.Registration
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
      RSR to getRsrPoints(deliusAssessments),
      ROSH to getRoshPoints(registrations),
      MAPPA to getMappaPoints(registrations),
      COMPLEXITY to getComplexityPoints(registrations),
      ADDITIONAL_FACTORS_FOR_WOMEN to additionalFactorsForWomen.calculate(crn, convictions, offenderAssessment)
    )

    val total = points.map { it.value }.sum()
      .minus(minOf(points.getOrDefault(RSR, 0), points.getOrDefault(ROSH, 0)))

    return when {
      total >= 30 -> TierLevel(A, total, points)
      total in 20..29 -> TierLevel(B, total, points)
      total in 10..19 -> TierLevel(C, total, points)
      else -> TierLevel(D, total, points)
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
          VERY_HIGH -> 30
          HIGH -> 20
          MEDIUM -> 10
          else -> 0
        }
      }

  private fun getMappaPoints(registrations: Collection<Registration>): Int =
    registrations
      .mapNotNull { Mappa.from(it.registerLevel?.code) }
      .firstOrNull()
      .let { mappa ->
        when (mappa) {
          M3, M2 -> 30
          M1 -> 5
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
