package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

import java.math.BigDecimal

enum class RsrThresholds(val num: BigDecimal) {
  TIER_B_RSR(BigDecimal(6.9)),
  TIER_C_RSR(BigDecimal(2.9))
}