package uk.gov.justice.digital.hmpps.hmppstier.domain

import uk.gov.justice.digital.hmpps.hmppstier.client.SentenceDto
import java.time.LocalDate

data class Sentence constructor(
  val terminationDate: LocalDate?,
  val sentenceType: String,
) {

  companion object {

    fun from(dto: SentenceDto): Sentence =
      Sentence(
        dto.terminationDate,
        dto.sentenceType.code,
      )
  }
}
