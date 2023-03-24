package uk.gov.justice.digital.hmpps.hmppstier.domain

import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.CalculationRule
import java.io.Serializable

data class TierLevel<E : Enum<E>>(
  val tier: E,
  val points: Int,
  val pointsBreakdown: Map<CalculationRule, Int>,
) : Serializable
