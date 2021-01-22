package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NsiTest {

  @Nested
  @DisplayName("Values test")
  inner class ValuesTest {

    @Test
    fun `It should match BRE08`() {
      assertThat(NsiStatus.from("BRE08")).isEqualTo(NsiStatus.BRE08)
    }

    @Test
    fun `It should match BRE15`() {
      assertThat(NsiStatus.from("BRE15")).isEqualTo(NsiStatus.BRE15)
    }

    @Test
    fun `It should match BRE16`() {
      assertThat(NsiStatus.from("BRE16")).isEqualTo(NsiStatus.BRE16)
    }

    @Test
    fun `It should match BRE09`() {
      assertThat(NsiStatus.from("BRE09")).isEqualTo(NsiStatus.BRE09)
    }

    @Test
    fun `It should match BRE24`() {
      assertThat(NsiStatus.from("BRE24")).isEqualTo(NsiStatus.BRE24)
    }

    @Test
    fun `It should match BRE25`() {
      assertThat(NsiStatus.from("BRE25")).isEqualTo(NsiStatus.BRE25)
    }

    @Test
    fun `null in null out`() {
      assertThat(NsiStatus.from(null)).isEqualTo(null)
    }

    @Test
    fun `invalid in null out`() {
      assertThat(NsiStatus.from("FISH")).isEqualTo(null)
    }
  }

  @Nested
  @DisplayName("Case Insensitive From")
  inner class CaseInsensitiveFrom {
    @Test
    fun `It should match case insensitive lower`() {
      assertThat(NsiStatus.from("bre08")).isEqualTo(NsiStatus.BRE08)
    }

    @Test
    fun `It should match case insensitive upper`() {
      assertThat(NsiStatus.from("BRE08")).isEqualTo(NsiStatus.BRE08)
    }

    @Test
    fun `It should match case insensitive mixed`() {
      assertThat(NsiStatus.from("BrE08")).isEqualTo(NsiStatus.BRE08)
    }
  }
}
