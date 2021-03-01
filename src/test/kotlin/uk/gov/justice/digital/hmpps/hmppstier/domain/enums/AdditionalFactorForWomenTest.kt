package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AdditionalFactorForWomenTest {

  @Nested
  @DisplayName("Sections test")
  inner class SectionsTest {

    @Test
    fun `PARENTING_RESPONSIBILITIES should be in section 13`() {
      assertThat(AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES.section).isEqualTo("6")
    }

    @Test
    fun `IMPULSIVITY should be in section 11`() {
      assertThat(AdditionalFactorForWomen.IMPULSIVITY.section).isEqualTo("11")
    }

    @Test
    fun `TEMPER_CONTROL should be in section 11`() {
      assertThat(AdditionalFactorForWomen.TEMPER_CONTROL.section).isEqualTo("11")
    }
  }

  @Nested
  @DisplayName("Values test")
  inner class ValuesTest {

    @Test
    fun `It should match PARENTING_RESPONSIBILITIES`() {
      assertThat(AdditionalFactorForWomen.from("6.9")).isEqualTo(AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES)
    }

    @Test
    fun `It should match IMPULSIVITY`() {
      assertThat(AdditionalFactorForWomen.from("11.2")).isEqualTo(AdditionalFactorForWomen.IMPULSIVITY)
    }

    @Test
    fun `It should match TEMPER_CONTROL`() {
      assertThat(AdditionalFactorForWomen.from("11.4")).isEqualTo(AdditionalFactorForWomen.TEMPER_CONTROL)
    }

    @Test
    fun `null in null out`() {
      assertThat(ComplexityFactor.from(null)).isEqualTo(null)
    }

    @Test
    fun `invalid in null out`() {
      assertThat(ComplexityFactor.from("FISH")).isEqualTo(null)
    }
  }

  @Nested
  @DisplayName("Case Insensitive From")
  inner class CaseInsensitiveFrom {
    @Test
    fun `It should match case insensitive lower`() {
      assertThat(AdditionalFactorForWomen.from("6.9")).isEqualTo(AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES)
    }

    @Test
    fun `It should match case insensitive upper`() {
      assertThat(AdditionalFactorForWomen.from("6.9")).isEqualTo(AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES)
    }
  }
}
