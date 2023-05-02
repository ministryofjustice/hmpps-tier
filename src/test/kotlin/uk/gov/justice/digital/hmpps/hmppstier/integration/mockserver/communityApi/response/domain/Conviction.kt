package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.domain

import java.time.LocalDate

data class Conviction(
  val id: Long = 2500222290,
  val active: Boolean = true,
  val convictionDate: LocalDate = LocalDate.of(2021,1,19),
  val sentence: Sentence? = Sentence(1)
)

data class Sentence(
  val sentenceLength: Long = 1,
  val sentenceCode: String = "NC",
  val terminationDate: LocalDate? = null
)