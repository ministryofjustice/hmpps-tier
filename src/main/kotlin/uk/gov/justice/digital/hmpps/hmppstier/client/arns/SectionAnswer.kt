package uk.gov.justice.digital.hmpps.hmppstier.client.arns

import uk.gov.justice.digital.hmpps.hmppstier.config.deserializer.SectionAnswerDeserialiserJackson2
import uk.gov.justice.digital.hmpps.hmppstier.config.deserializer.SectionAnswerDeserialiserJackson3
import com.fasterxml.jackson.databind.annotation.JsonDeserialize as Jackson2JsonDeserialize
import tools.jackson.databind.annotation.JsonDeserialize as Jackson3JsonDeserialize

@Jackson2JsonDeserialize(using = SectionAnswerDeserialiserJackson2::class)
@Jackson3JsonDeserialize(using = SectionAnswerDeserialiserJackson3::class)
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