package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.response.domain

import java.time.LocalDateTime

data class Assessment(
  val completedDate: LocalDateTime,
  val assessmentId: Long,
  val status: String,
)
