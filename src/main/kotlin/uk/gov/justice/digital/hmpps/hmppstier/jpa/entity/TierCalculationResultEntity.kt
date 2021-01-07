package uk.gov.justice.digital.hmpps.hmppstier.jpa.entity

import uk.gov.justice.digital.hmpps.hmppstier.domain.TierCalculationResult
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierResult
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeScore
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectScore

data class TierCalculationResultEntity(
  val protect: TierResult<ProtectScore>,

  val change: TierResult<ChangeScore>,
) {

  companion object {
    fun from(tierCalculationResult: TierCalculationResult): TierCalculationResultEntity {
      return TierCalculationResultEntity(tierCalculationResult.protectScore, tierCalculationResult.changeScore)
    }
  }
}

