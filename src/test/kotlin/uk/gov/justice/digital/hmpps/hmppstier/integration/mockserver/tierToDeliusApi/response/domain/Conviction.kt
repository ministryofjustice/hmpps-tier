package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain

import java.time.LocalDate

data class Conviction(
  val breached: Boolean = false,
  val requirements: MutableList<Requirement> = mutableListOf(),
  val sentenceCode: String = "NC",
  val terminationDate: LocalDate? = null,
)
