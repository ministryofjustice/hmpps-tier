package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MappaTest {

  @Nested
  @DisplayName("Values test")
  inner class ValuesTest {
    @Test
    fun `It should find M1`() {
      assertThat(Mappa.from("M1")).isEqualTo(Mappa.M1)
    }

    @Test
    fun `It should find M2`() {
      assertThat(Mappa.from("M2")).isEqualTo(Mappa.M2)
    }

    @Test
    fun `It should find M3`() {
      assertThat(Mappa.from("M3")).isEqualTo(Mappa.M3)
    }

    @Test
    fun `null in null out`() {
      assertThat(Mappa.from(null)).isEqualTo(null)
    }

    @Test
    fun `invalid in null out`() {
      assertThat(Mappa.from("FISH")).isEqualTo(null)
    }
  }

  @Nested
  @DisplayName("Case Insensitive From")
  inner class CaseInsensitiveFrom {
    @Test
    fun `It should match case insensitive lower`() {
      assertThat(Mappa.from("m1")).isEqualTo(Mappa.M1)
    }

    @Test
    fun `It should match case insensitive upper`() {
      assertThat(Mappa.from("M1")).isEqualTo(Mappa.M1)
    }

    @Test
    fun `It should return null if code is wrong or misspelled`() {
      assertThat(Mappa.from("Invalid")).isNull()
    }
  }
}
