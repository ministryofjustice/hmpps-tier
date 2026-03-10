package uk.gov.justice.digital.hmpps.hmppstier.messaging.consumer

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonProperty

data class SQSMessage(
    @JsonProperty("Message") val message: String,
    @JsonProperty("MessageAttributes") val attributes: MessageAttributes = MessageAttributes(),
) {
    data class MessageAttributes(
        @JsonAnyGetter @JsonAnySetter
        private val attributes: MutableMap<String, MessageAttribute> = mutableMapOf(),
    ) : MutableMap<String, MessageAttribute> by attributes {

        val eventType = attributes[EVENT_TYPE_KEY]?.value

        companion object {
            private const val EVENT_TYPE_KEY = "eventType"
        }
    }

    data class MessageAttribute(@JsonProperty("Type") val type: String, @JsonProperty("Value") val value: String)
}