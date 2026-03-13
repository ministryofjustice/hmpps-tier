package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class MappaLevelTest {

    @ParameterizedTest(name = "It should find {0} {1} type code")
    @MethodSource("getMappaCombinations")
    fun `It should find mappa`(value: String, typeCode: String, expectedMappaLevel: MappaLevel) {
        assertThat(MappaLevel.from(value, typeCode)).isEqualTo(expectedMappaLevel)
    }

    @Test
    fun `null in null out`() {
        assertThat(MappaLevel.from(null, "MAPP")).isEqualTo(null)
    }

    @Test
    fun `invalid in null out`() {
        assertThat(MappaLevel.from("FISH", "MAPP")).isEqualTo(null)
    }

    @Test
    fun `null when type code is null`() {
        assertThat(MappaLevel.from("M1", null)).isEqualTo(null)
    }

    @Test
    fun `null when type code is invalid`() {
        assertThat(MappaLevel.from("M1", "FISH")).isEqualTo(null)
    }

    @Nested
    @DisplayName("Case Insensitive From")
    inner class CaseInsensitiveFrom {
        @Test
        fun `It should match case insensitive lower`() {
            assertThat(MappaLevel.from("m1", "MAPP")).isEqualTo(MappaLevel.M1)
        }

        @Test
        fun `It should match case insensitive upper`() {
            assertThat(MappaLevel.from("M1", "MAPP")).isEqualTo(MappaLevel.M1)
        }
    }

    companion object {
        @JvmStatic
        fun getMappaCombinations(): List<Arguments> {
            return listOf(
                Arguments.of("M1", "MAPP", MappaLevel.M1),
                Arguments.of("M2", "MAPP", MappaLevel.M2),
                Arguments.of("M3", "MAPP", MappaLevel.M3),
            )
        }
    }
}
