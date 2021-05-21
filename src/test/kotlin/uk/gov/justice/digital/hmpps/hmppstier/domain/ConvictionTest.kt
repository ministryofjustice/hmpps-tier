package uk.gov.justice.digital.hmpps.hmppstier.domain

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.client.ConvictionDto
import uk.gov.justice.digital.hmpps.hmppstier.client.KeyValue
import uk.gov.justice.digital.hmpps.hmppstier.client.Offence
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenceDetail
import uk.gov.justice.digital.hmpps.hmppstier.client.SentenceDto
import java.time.LocalDate

internal class ConvictionTest {

  @Nested
  @DisplayName("From test")
  inner class FromTest {

    @Test
    fun `It should construct using from`() {
      val now = LocalDate.now()
      val sentenceDto = SentenceDto(now.plusDays(1), KeyValue("SC"), now, now.plusDays(3))
      val convictionDto = ConvictionDto(123L, sentenceDto, listOf(Offence(OffenceDetail("BR4"))), KeyValue("302"))
      val conviction = Conviction.from(convictionDto)
      Assertions.assertThat(conviction.convictionId).isEqualTo(convictionDto.convictionId)
      Assertions.assertThat(conviction.offenceMainCategoryCodes).isEqualTo(convictionDto.offences.map { it.offenceDetail.mainCategoryCode })
      Assertions.assertThat(conviction.latestCourtAppearanceOutcome).isEqualTo(convictionDto.latestCourtAppearanceOutcome?.code)

      Assertions.assertThat(conviction.sentence.expectedEndDate).isEqualTo(convictionDto.sentence?.expectedSentenceEndDate)
      Assertions.assertThat(conviction.sentence.sentenceType).isEqualTo(convictionDto.sentence?.sentenceType?.code)
      Assertions.assertThat(conviction.sentence.startDate).isEqualTo(convictionDto.sentence?.startDate)
      Assertions.assertThat(conviction.sentence.terminationDate).isEqualTo(convictionDto.sentence?.terminationDate)
    }

    @Test
    fun `It should construct using from null values`() {
      val now = LocalDate.now()
      val sentenceDto = SentenceDto(null, KeyValue("SC"), now, null)
      val convictionDto = ConvictionDto(123L, sentenceDto, listOf(), KeyValue("302"))
      val conviction = Conviction.from(convictionDto)
      Assertions.assertThat(conviction.convictionId).isEqualTo(convictionDto.convictionId)
      Assertions.assertThat(conviction.offenceMainCategoryCodes).isEqualTo(convictionDto.offences.map { it.offenceDetail.mainCategoryCode })
      Assertions.assertThat(conviction.latestCourtAppearanceOutcome).isEqualTo(convictionDto.latestCourtAppearanceOutcome?.code)

      Assertions.assertThat(conviction.sentence.expectedEndDate).isEqualTo(convictionDto.sentence?.expectedSentenceEndDate)
      Assertions.assertThat(conviction.sentence.sentenceType).isEqualTo(convictionDto.sentence?.sentenceType?.code)
      Assertions.assertThat(conviction.sentence.startDate).isEqualTo(convictionDto.sentence?.startDate)
      Assertions.assertThat(conviction.sentence.terminationDate).isEqualTo(convictionDto.sentence?.terminationDate)
    }
  }
}
