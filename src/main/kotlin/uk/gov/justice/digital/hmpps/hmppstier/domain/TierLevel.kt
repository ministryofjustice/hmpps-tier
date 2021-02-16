package uk.gov.justice.digital.hmpps.hmppstier.domain

import java.io.Serializable

data class TierLevel<E : Enum<E>>(
  val tier: E,
  val points: Int,
) : Serializable
