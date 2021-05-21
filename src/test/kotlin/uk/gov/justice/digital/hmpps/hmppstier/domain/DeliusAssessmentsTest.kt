package uk.gov.justice.digital.hmpps.hmppstier.domain

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusAssessmentsDto
import java.math.BigDecimal

internal class DeliusAssessmentsTest {

  @Nested
  @DisplayName("From test")
  inner class FromTest {

    @Test
    fun `It should construct using from`() {
      val deliusAssessmentsDto = DeliusAssessmentsDto(BigDecimal.TEN, 50)
      val deliusAssessments = DeliusAssessments.from(deliusAssessmentsDto)
      Assertions.assertThat(deliusAssessments.rsr).isEqualTo(deliusAssessmentsDto.rsr)
      Assertions.assertThat(deliusAssessments.ogrs).isEqualTo(deliusAssessmentsDto.ogrs)
    }

    @Test
    fun `It should construct using from null values`() {
      val deliusAssessments = DeliusAssessments.from(DeliusAssessmentsDto(null, null))
      Assertions.assertThat(deliusAssessments.rsr).isEqualTo(BigDecimal.ZERO)
      Assertions.assertThat(deliusAssessments.ogrs).isEqualTo(0)
    }

    @Test
    fun `It should construct using from null`() {
      val deliusAssessments = DeliusAssessments.from(null)
      Assertions.assertThat(deliusAssessments.rsr).isEqualTo(BigDecimal.ZERO)
      Assertions.assertThat(deliusAssessments.ogrs).isEqualTo(0)
    }
  }
}
