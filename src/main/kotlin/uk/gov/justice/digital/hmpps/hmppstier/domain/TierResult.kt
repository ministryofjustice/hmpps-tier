package uk.gov.justice.digital.hmpps.hmppstier.domain

import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.TierMatchCriteria

data class TierResult<E : Enum<E>>(
  val tier: E,
  val score: Int,
  val criteria: Set<TierMatchCriteria>
)
