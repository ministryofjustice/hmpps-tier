package uk.gov.justice.digital.hmpps.hmppstier.domain

import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity

data class ChangeScores(
  val crn: String,
  val ogrsScore: Int?,
  val need: Map<Need, NeedSeverity?>,
)
