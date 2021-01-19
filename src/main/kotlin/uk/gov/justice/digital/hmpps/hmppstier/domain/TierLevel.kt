package uk.gov.justice.digital.hmpps.hmppstier.domain

data class TierLevel<E : Enum<E>>(
  val tier: E,
  val points: Int,
)
