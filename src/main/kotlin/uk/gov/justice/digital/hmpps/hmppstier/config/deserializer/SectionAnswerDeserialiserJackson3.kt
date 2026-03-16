package uk.gov.justice.digital.hmpps.hmppstier.config.deserializer

import org.springframework.boot.jackson.JacksonComponent
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ValueDeserializer
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.SectionAnswer

@JacksonComponent
class SectionAnswerDeserialiserJackson3 : ValueDeserializer<SectionAnswer>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext?): SectionAnswer? {
        val json: JsonNode = parser.readValueAsTree()
        if (json.isNull || json.asString().isEmpty()) return null
        return when (json.asString()) {
            in PROBLEM -> SectionAnswer.Problem.entries.firstOrNull { it.name == json.asString() }
            in YES_NO -> SectionAnswer.YesNo.entries.firstOrNull { it.name == json.asString() }
            in FREQUENCY -> SectionAnswer.Frequency.entries.firstOrNull { it.name == json.asString() }
            else -> throw IllegalArgumentException("Section Answer not recognised")
        }
    }

    companion object {
        private val PROBLEM = SectionAnswer.Problem.entries.map { it.name }
        private val YES_NO = SectionAnswer.YesNo.entries.map { it.name }
        private val FREQUENCY = SectionAnswer.Frequency.entries.map { it.name }
    }
}