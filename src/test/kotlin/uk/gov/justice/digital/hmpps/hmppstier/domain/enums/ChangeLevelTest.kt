package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ChangeLevelTest {

    @Nested
    @DisplayName("Values test")
    inner class ValuesTest {

        @Test
        fun `It should match 0`() {
            assertThat(ChangeLevel.ZERO.value).isEqualTo(0)
        }

        @Test
        fun `It should match 1`() {
            assertThat(ChangeLevel.ONE.value).isEqualTo(1)
        }

        @Test
        fun `It should match 2`() {
            assertThat(ChangeLevel.TWO.value).isEqualTo(2)
        }

        @Test
        fun `It should match 3`() {
            assertThat(ChangeLevel.THREE.value).isEqualTo(3)
        }
    }
}
