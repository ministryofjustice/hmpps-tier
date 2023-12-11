package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ProtectLevelTest {

    @Nested
    @DisplayName("Values test")
    inner class ValuesTest {

        @Test
        fun `It should match A`() {
            assertThat(ProtectLevel.A.value).isEqualTo("A")
        }

        @Test
        fun `It should match B`() {
            assertThat(ProtectLevel.B.value).isEqualTo("B")
        }

        @Test
        fun `It should match C`() {
            assertThat(ProtectLevel.C.value).isEqualTo("C")
        }

        @Test
        fun `It should match D`() {
            assertThat(ProtectLevel.D.value).isEqualTo("D")
        }
    }
}
