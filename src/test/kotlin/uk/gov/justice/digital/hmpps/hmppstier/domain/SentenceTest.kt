package uk.gov.justice.digital.hmpps.hmppstier.domain

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.client.KeyValue
import uk.gov.justice.digital.hmpps.hmppstier.client.SentenceDto
import java.time.LocalDate

internal class SentenceTest {

  @Nested
  @DisplayName("From test")
  inner class FromTest {

    @Test
    fun `It should construct using from`() {
      val now = LocalDate.now()
      val sentenceDto = SentenceDto(now.plusDays(1), KeyValue("SC"), now, now.plusDays(3))
      val sentence = Sentence.from(sentenceDto)

      Assertions.assertThat(sentence.expectedSentenceEndDate).isEqualTo(sentenceDto.expectedSentenceEndDate)
      Assertions.assertThat(sentence.sentenceType).isEqualTo(sentenceDto.sentenceType.code)
      Assertions.assertThat(sentence.startDate).isEqualTo(sentenceDto.startDate)
      Assertions.assertThat(sentence.terminationDate).isEqualTo(sentenceDto.terminationDate)
    }

    @Test
    fun `It should construct using from null values`() {
      val now = LocalDate.now()
      val sentenceDto = SentenceDto(null, KeyValue("SC"), now, null)
      val sentence = Sentence.from(sentenceDto)

      Assertions.assertThat(sentence.expectedSentenceEndDate).isEqualTo(sentenceDto.expectedSentenceEndDate)
      Assertions.assertThat(sentence.sentenceType).isEqualTo(sentenceDto.sentenceType.code)
      Assertions.assertThat(sentence.startDate).isEqualTo(sentenceDto.startDate)
      Assertions.assertThat(sentence.terminationDate).isEqualTo(sentenceDto.terminationDate)
    }
  }
}
