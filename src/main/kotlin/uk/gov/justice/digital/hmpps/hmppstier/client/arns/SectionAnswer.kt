package uk.gov.justice.digital.hmpps.hmppstier.client.arns

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import uk.gov.justice.digital.hmpps.hmppstier.config.deserializer.SectionAnswerDeserialiser

@JsonDeserialize(using = SectionAnswerDeserialiser::class)
sealed interface SectionAnswer {
    val score: Int

    enum class YesNo(override val score: Int) : SectionAnswer {
        Yes(2), No(0), Unknown(0)
    }

    enum class Problem(override val score: Int) : SectionAnswer {
        None(0), Some(1), Significant(2), Missing(0)
    }

    enum class Frequency(override val score: Int) : SectionAnswer {
        Never(0), Previous(1), Currently(2), Unknown(0)
    }
}