package uk.gov.justice.digital.hmpps.hmppstier.domain

import uk.gov.justice.digital.hmpps.hmppstier.client.SentenceDto
import java.time.LocalDate

data class Sentence constructor(
  val sentenceType: String,
  val startDate: LocalDate,
  val expectedSentenceEndDate: LocalDate?,
  val terminationDate: LocalDate?,
) {

  companion object {

    fun from(dto: SentenceDto): Sentence =
      Sentence(
        dto.sentenceType.code,
        dto.startDate,
        dto.expectedSentenceEndDate,
        dto.terminationDate
      )
  }
}
