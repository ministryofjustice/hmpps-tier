package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NsiOutcomeTest {

    @Nested
    @DisplayName("Values test")
    inner class ValuesTest {

        @Test
        fun `It should match BRE01`() {
            assertThat(NsiOutcome.from("BRE01")).isEqualTo(NsiOutcome.BRE01)
        }

        @Test
        fun `It should match BRE02`() {
            assertThat(NsiOutcome.from("BRE02")).isEqualTo(NsiOutcome.BRE02)
        }

        @Test
        fun `It should match BRE03`() {
            assertThat(NsiOutcome.from("BRE03")).isEqualTo(NsiOutcome.BRE03)
        }

        @Test
        fun `It should match BRE04`() {
            assertThat(NsiOutcome.from("BRE04")).isEqualTo(NsiOutcome.BRE04)
        }

        @Test
        fun `It should match BRE05`() {
            assertThat(NsiOutcome.from("BRE05")).isEqualTo(NsiOutcome.BRE05)
        }

        @Test
        fun `It should match BRE06`() {
            assertThat(NsiOutcome.from("BRE06")).isEqualTo(NsiOutcome.BRE06)
        }

        @Test
        fun `It should match BRE07`() {
            assertThat(NsiOutcome.from("BRE07")).isEqualTo(NsiOutcome.BRE07)
        }

        @Test
        fun `It should match BRE08`() {
            assertThat(NsiOutcome.from("BRE08")).isEqualTo(NsiOutcome.BRE08)
        }

        @Test
        fun `It should match BRE10`() {
            assertThat(NsiOutcome.from("BRE10")).isEqualTo(NsiOutcome.BRE10)
        }

        @Test
        fun `It should match BRE13`() {
            assertThat(NsiOutcome.from("BRE13")).isEqualTo(NsiOutcome.BRE13)
        }

        @Test
        fun `It should match BRE14`() {
            assertThat(NsiOutcome.from("BRE14")).isEqualTo(NsiOutcome.BRE14)
        }

        @Test
        fun `It should match BRE16`() {
            assertThat(NsiOutcome.from("BRE16")).isEqualTo(NsiOutcome.BRE16)
        }

        @Test
        fun `It should match REC01`() {
            assertThat(NsiOutcome.from("REC01")).isEqualTo(NsiOutcome.REC01)
        }

        @Test
        fun `It should match REC02`() {
            assertThat(NsiOutcome.from("REC02")).isEqualTo(NsiOutcome.REC02)
        }

        @Test
        fun `null in null out`() {
            assertThat(NsiOutcome.from(null)).isEqualTo(null)
        }

        @Test
        fun `invalid in null out`() {
            assertThat(NsiOutcome.from("FISH")).isEqualTo(null)
        }
    }

    @Nested
    @DisplayName("Case Insensitive From")
    inner class CaseInsensitiveFrom {
        @Test
        fun `It should match case insensitive lower`() {
            assertThat(NsiOutcome.from("bre08")).isEqualTo(NsiOutcome.BRE08)
        }

        @Test
        fun `It should match case insensitive upper`() {
            assertThat(NsiOutcome.from("BRE08")).isEqualTo(NsiOutcome.BRE08)
        }

        @Test
        fun `It should match case insensitive mixed`() {
            assertThat(NsiOutcome.from("BrE08")).isEqualTo(NsiOutcome.BRE08)
        }
    }
}
