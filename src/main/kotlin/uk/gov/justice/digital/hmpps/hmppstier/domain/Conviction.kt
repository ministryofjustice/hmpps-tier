package uk.gov.justice.digital.hmpps.hmppstier.domain

import uk.gov.justice.digital.hmpps.hmppstier.client.ConvictionDto

data class Conviction constructor(
  val convictionId: Long,
  val sentence: Sentence,
) {

  companion object {

    fun from(dto: ConvictionDto): Conviction =
      Conviction(
        dto.convictionId,
        Sentence.from(dto.sentence!!)
      )
  }
}
