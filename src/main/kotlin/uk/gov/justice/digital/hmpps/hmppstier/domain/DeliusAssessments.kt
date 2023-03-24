package uk.gov.justice.digital.hmpps.hmppstier.domain

import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusAssessmentsDto
import java.math.BigDecimal
import java.math.BigDecimal.ZERO

data class DeliusAssessments(
  val rsr: BigDecimal,
  val ogrs: Int,
) {

  companion object {
    fun from(dto: DeliusAssessmentsDto?): DeliusAssessments {
      return DeliusAssessments(
        rsr = dto?.rsr ?: ZERO,
        ogrs = dto?.ogrs ?: 0,
      )
    }
  }
}
