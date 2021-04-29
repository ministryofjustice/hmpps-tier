package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class RSRTest {

  @Nested
  @DisplayName("Values test")
  inner class ValuesTest {

    @Test
    fun `It should match RSR C Lower`() {
      assertThat(RsrThresholds.TIER_C_RSR_LOWER.num).isEqualTo(BigDecimal(3.00))
    }

    @Test
    fun `It should match RSR C Upper`() {
      assertThat(RsrThresholds.TIER_C_RSR_UPPER.num).isEqualTo(BigDecimal(6.99))
    }

    @Test
    fun `It should match RSR B Lower`() {
      assertThat(RsrThresholds.TIER_B_RSR_LOWER.num).isEqualTo(BigDecimal(7.00))
    }

    @Test
    fun `It should match RSR B Upper`() {
      assertThat(RsrThresholds.TIER_B_RSR_UPPER.num).isEqualTo(BigDecimal(99.99))
    }
  }
}
