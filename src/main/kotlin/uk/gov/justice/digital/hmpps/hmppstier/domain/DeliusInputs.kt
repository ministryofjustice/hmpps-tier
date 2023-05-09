package uk.gov.justice.digital.hmpps.hmppstier.domain

import java.math.BigDecimal

data class DeliusInputs(
  val isFemale: Boolean,
  val rsrScore: BigDecimal,
  val ogrsScore: Int,
  val breached: Boolean,
  val hasNoMandate: Boolean,
)
