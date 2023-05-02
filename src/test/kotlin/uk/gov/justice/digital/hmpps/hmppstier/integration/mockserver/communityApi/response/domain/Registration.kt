package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.domain

import java.time.LocalDate

data class Registration(
  val registerLevel: String? = null,
  val typeCode: String = "MAPP",
  val startDate: LocalDate = LocalDate.of(2021, 2, 1)
)