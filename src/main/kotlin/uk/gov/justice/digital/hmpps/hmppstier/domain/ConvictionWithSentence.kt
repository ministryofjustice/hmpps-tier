package uk.gov.justice.digital.hmpps.hmppstier.domain

import uk.gov.justice.digital.hmpps.hmppstier.client.ConvictionDto

data class ConvictionWithSentence constructor(
  val convictionId: Long,
  val sentence: Sentence,
  val offences: List<Offence>,
  val latestCourtAppearanceOutcome: String?
) {

  companion object {
    fun from(dto: List<ConvictionDto>): List<ConvictionWithSentence> = dto.map { from(it) }

    private fun from(dto: ConvictionDto): ConvictionWithSentence =
      ConvictionWithSentence(
        dto.convictionId,
        dto.sentence,
        dto.offences.filterNotNull(),
        dto.latestCourtAppearanceOutcome?.code
      )
  }
}
