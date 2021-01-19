package uk.gov.justice.digital.hmpps.hmppstier.jpa.entity

import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel

data class TierCalculationResultEntity(
  val protect: TierLevel<ProtectLevel>,
  val change: TierLevel<ChangeLevel>)
