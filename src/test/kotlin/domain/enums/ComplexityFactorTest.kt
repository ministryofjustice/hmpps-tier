package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ComplexityFactorTest {

  @Nested
  @DisplayName("Values test")
  inner class ValuesTest {

    @Test
    fun `It should match IOM_NOMINAL`() {
      assertThat(ComplexityFactor.from("IIOM")).isEqualTo(ComplexityFactor.IOM_NOMINAL)
    }

    @Test
    fun `It should match MENTAL_HEALTH`() {
      assertThat(ComplexityFactor.from("RMDO")).isEqualTo(ComplexityFactor.MENTAL_HEALTH)
    }

    @Test
    fun `It should match ATTEMPTED_SUICIDE_OR_SELF_HARM`() {
      assertThat(ComplexityFactor.from("ALSH")).isEqualTo(ComplexityFactor.ATTEMPTED_SUICIDE_OR_SELF_HARM)
    }

    @Test
    fun `It should match VULNERABILITY_ISSUE`() {
      assertThat(ComplexityFactor.from("RVLN")).isEqualTo(ComplexityFactor.VULNERABILITY_ISSUE)
    }

    @Test
    fun `It should match CHILD_CONCERNS`() {
      assertThat(ComplexityFactor.from("RCCO")).isEqualTo(ComplexityFactor.CHILD_CONCERNS)
    }

    @Test
    fun `It should match CHILD_PROTECTION`() {
      assertThat(ComplexityFactor.from("RCPR")).isEqualTo(ComplexityFactor.CHILD_PROTECTION)
    }

    @Test
    fun `It should match RISK_TO_CHILDREN`() {
      assertThat(ComplexityFactor.from("RCHD")).isEqualTo(ComplexityFactor.RISK_TO_CHILDREN)
    }

    @Test
    fun `It should match PUBLIC_INTEREST`() {
      assertThat(ComplexityFactor.from("RPIR")).isEqualTo(ComplexityFactor.PUBLIC_INTEREST)
    }

    @Test
    fun `It should match ADULT_AT_RISK`() {
      assertThat(ComplexityFactor.from("RVAD")).isEqualTo(ComplexityFactor.ADULT_AT_RISK)
    }

    @Test
    fun `It should match STREET_GANGS`() {
      assertThat(ComplexityFactor.from("STRG")).isEqualTo(ComplexityFactor.STREET_GANGS)
    }

    @Test
    fun `It should match TERRORISM`() {
      assertThat(ComplexityFactor.from("RTAO")).isEqualTo(ComplexityFactor.TERRORISM)
    }
  }

  @Nested
  @DisplayName("Case Insensitive From")
  inner class CaseInsensitiveFrom {
    @Test
    fun `It should match case insensitive lower`() {
      assertThat(ComplexityFactor.from("rtao")).isEqualTo(ComplexityFactor.TERRORISM)
    }

    @Test
    fun `It should match case insensitive upper`() {
      assertThat(ComplexityFactor.from("RTAO")).isEqualTo(ComplexityFactor.TERRORISM)
    }

    @Test
    fun `It should match case insensitive mixed`() {
      assertThat(ComplexityFactor.from("RtAO")).isEqualTo(ComplexityFactor.TERRORISM)
    }

    @Test
    fun `It should return null if code is wrong or misspelled`() {
      assertThat(ComplexityFactor.from("Invalid")).isNull()
    }
  }
}
