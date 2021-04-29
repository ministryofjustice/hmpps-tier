package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

import java.math.BigDecimal

enum class RsrThresholds(val num: BigDecimal) {
  TIER_B_RSR(BigDecimal(7.0)),
  TIER_C_RSR(BigDecimal(3.0)),
  NO_RSR_MAGIC_NUMBER(BigDecimal(999.99))
}
