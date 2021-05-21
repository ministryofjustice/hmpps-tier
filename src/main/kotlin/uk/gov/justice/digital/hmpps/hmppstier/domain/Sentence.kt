package uk.gov.justice.digital.hmpps.hmppstier.domain

import uk.gov.justice.digital.hmpps.hmppstier.client.SentenceDto
import java.time.LocalDate

data class Sentence constructor(
  val terminationDate: LocalDate?,
  val sentenceType: String,
  val startDate: LocalDate,
  val expectedEndDate: LocalDate?,
) {

  fun isCustodial(): Boolean = this.sentenceType in arrayOf("NC", "SC")

  companion object {

    fun from(dto: SentenceDto): Sentence =
      Sentence(
        dto.terminationDate,
        dto.sentenceType.code,
        dto.startDate,
        dto.expectedSentenceEndDate
      )
  }
}
