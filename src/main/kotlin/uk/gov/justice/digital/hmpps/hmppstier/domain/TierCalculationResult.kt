package uk.gov.justice.digital.hmpps.hmppstier.domain

import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeScore
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectScore

data class TierCalculationResult(

  val protectScore: TierResult<ProtectScore>,

  val changeScore: TierResult<ChangeScore>,
)