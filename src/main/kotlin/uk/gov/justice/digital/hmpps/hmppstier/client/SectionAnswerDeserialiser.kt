package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.jackson.JsonComponent
import uk.gov.justice.digital.hmpps.hmppstier.client.SectionAnswer.*
import java.io.IOException

@JsonComponent
class SectionAnswerDeserialiser : JsonDeserializer<SectionAnswer>() {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(parser: JsonParser, context: DeserializationContext?): SectionAnswer? {
        val mapper = parser.codec as ObjectMapper
        val json: JsonNode = mapper.readTree(parser)
        if (json.isNull || json.asText().isEmpty()) return null
        return when (json.asText()) {
            in PROBLEM -> Problem.entries.firstOrNull { it.name == json.asText() }
            in YES_NO -> YesNo.entries.firstOrNull { it.name == json.asText() }
            in FREQUENCY -> Frequency.entries.firstOrNull { it.name == json.asText() }
            else -> throw IllegalArgumentException("Section Answer not recognised")
        }
    }

    companion object {
        private val PROBLEM = Problem.entries.map { it.name }
        private val YES_NO = YesNo.entries.map { it.name }
        private val FREQUENCY = Frequency.entries.map { it.name }
    }
}