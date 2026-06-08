package uk.gov.justice.digital.hmpps.hmppstier.service.calculation

import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier

data class CalculationResult(
    val tier: Tier,
    val provisional: Boolean = false,
)
