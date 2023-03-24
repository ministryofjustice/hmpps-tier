package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class MappaTest {

  @ParameterizedTest(name = "It should find {0} {1} type code")
  @MethodSource("getMappaCombinations")
  fun `It should find mappa`(value: String, typeCode: String, expectedMappa: Mappa) {
    assertThat(Mappa.from(value, typeCode)).isEqualTo(expectedMappa)
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

  companion object {
    @JvmStatic
    fun getMappaCombinations(): List<Arguments> {
      return listOf(
        Arguments.of("M1", "MAPP", Mappa.M1),
        Arguments.of("M2", "MAPP", Mappa.M2),
        Arguments.of("M3", "MAPP", Mappa.M3),
      )
    }
  }
}
