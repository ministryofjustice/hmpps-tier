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
      assertThat(Mappa.from("M1", "MAPP")).isEqualTo(Mappa.M1)
    }

    @Test
    fun `It should find M2`() {
      assertThat(Mappa.from("M2", "MAPP")).isEqualTo(Mappa.M2)
    }

    @Test
    fun `It should find M3`() {
      assertThat(Mappa.from("M3", "MAPP")).isEqualTo(Mappa.M3)
    }

    @Test
    fun `null in null out`() {
      assertThat(Mappa.from(null, "MAPP")).isEqualTo(null)
    }

    @Test
    fun `invalid in null out`() {
      assertThat(Mappa.from("FISH", "MAPP")).isEqualTo(null)
    }

    @Test
    fun `null when type code is null`() {
      assertThat(Mappa.from("M1", null)).isEqualTo(null)
    }

    @Test
    fun `null when type code is invalid`() {
      assertThat(Mappa.from("M1", "FISH")).isEqualTo(null)
    }
  }

  @Nested
  @DisplayName("Case Insensitive From")
  inner class CaseInsensitiveFrom {
    @Test
    fun `It should match case insensitive lower`() {
      assertThat(Mappa.from("m1", "MAPP")).isEqualTo(Mappa.M1)
    }

    @Test
    fun `It should match case insensitive upper`() {
      assertThat(Mappa.from("M1", "MAPP")).isEqualTo(Mappa.M1)
    }
  }
}
