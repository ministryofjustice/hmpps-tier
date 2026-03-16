package uk.gov.justice.digital.hmpps.hmppstier.config.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.jackson.JacksonComponent
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.SectionAnswer
import java.io.IOException

@JacksonComponent
class SectionAnswerDeserialiserJackson2 : JsonDeserializer<SectionAnswer>() {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(parser: JsonParser, context: DeserializationContext?): SectionAnswer? {
        val mapper = parser.codec as ObjectMapper
        val json: JsonNode = mapper.readTree(parser)
        if (json.isNull || json.asText().isEmpty()) return null
        return when (json.asText()) {
            in PROBLEM -> SectionAnswer.Problem.entries.firstOrNull { it.name == json.asText() }
            in YES_NO -> SectionAnswer.YesNo.entries.firstOrNull { it.name == json.asText() }
            in FREQUENCY -> SectionAnswer.Frequency.entries.firstOrNull { it.name == json.asText() }
            else -> throw IllegalArgumentException("Section Answer not recognised")
        }
    }

    companion object {
        private val PROBLEM = SectionAnswer.Problem.entries.map { it.name }
        private val YES_NO = SectionAnswer.YesNo.entries.map { it.name }
        private val FREQUENCY = SectionAnswer.Frequency.entries.map { it.name }
    }
}