package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RoshTest {

  @Nested
  @DisplayName("Values test")
  inner class ValuesTest {

    @Test
    fun `It should match Very High`() {
      assertThat(Rosh.from("RVHR")).isEqualTo(Rosh.VERY_HIGH)
    }

    @Test
    fun `It should match High`() {
      assertThat(Rosh.from("RHRH")).isEqualTo(Rosh.HIGH)
    }

    @Test
    fun `It should match Medium`() {
      assertThat(Rosh.from("RMRH")).isEqualTo(Rosh.MEDIUM)
    }

    @Test
    fun `null in null out`() {
      assertThat(Rosh.from(null)).isEqualTo(null)
    }

    @Test
    fun `invalid in null out`() {
      assertThat(Rosh.from("FISH")).isEqualTo(null)
    }
  }

  @Nested
  @DisplayName("Case Insensitive From")
  inner class CaseInsensitiveFrom {
    @Test
    fun `It should match case insensitive lower`() {
      assertThat(Rosh.from("rvhr")).isEqualTo(Rosh.VERY_HIGH)
    }

    @Test
    fun `It should match case insensitive upper`() {
      assertThat(Rosh.from("RVHR")).isEqualTo(Rosh.VERY_HIGH)
    }

    @Test
    fun `It should match case insensitive mixed`() {
      assertThat(Rosh.from("RvhR")).isEqualTo(Rosh.VERY_HIGH)
    }

  }
}
