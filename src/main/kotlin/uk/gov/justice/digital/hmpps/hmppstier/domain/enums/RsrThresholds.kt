package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

import java.math.BigDecimal

enum class RsrThresholds(val num: BigDecimal) {
    TIER_B_RSR_UPPER(BigDecimal(99.99)),
    TIER_B_RSR_LOWER(BigDecimal(7.00)),
    TIER_C_RSR_UPPER(BigDecimal(6.99)),
    TIER_C_RSR_LOWER(BigDecimal(3.00)),
}
