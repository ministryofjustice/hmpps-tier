package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AssessmentComplexityFactorTest {

  @Nested
  @DisplayName("Values test")
  inner class ValuesTest {

    @Test
    fun `It should match PARENTING_RESPONSIBILITIES`() {
      assertThat(AssessmentComplexityFactor.from("13.3 - F")).isEqualTo(AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES)
    }
  }

  @Nested
  @DisplayName("Case Insensitive From")
  inner class CaseInsensitiveFrom {
    @Test
    fun `It should match case insensitive lower`() {
      assertThat(AssessmentComplexityFactor.from("13.3 - f")).isEqualTo(AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES)
    }

    @Test
    fun `It should match case insensitive upper`() {
      assertThat(AssessmentComplexityFactor.from("13.3 - F")).isEqualTo(AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES)
    }
  }
}
