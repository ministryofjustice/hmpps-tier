package uk.gov.justice.digital.hmpps.hmppstier.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingTopicException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class SuccessUpdater(
    hmppsQueueService: HmppsQueueService,
    private val objectMapper: ObjectMapper,
    @Value("\${hmpps.tier.endpoint.url}") private val hmppsTierEndpointUrl: String,
) {

    private val calculationCompleteTopic = hmppsQueueService.findByTopicId("hmppscalculationcompletetopic")
        ?: throw MissingTopicException("Could not find topic hmppscalculationcompletetopic")

    fun update(crn: String, calculationId: UUID) {
        val eventType = "tier.calculation.complete"
        val detailUrl = "$hmppsTierEndpointUrl/crn/$crn/tier/$calculationId"
        val message = TierChangeEvent(
            eventType,
            2,
            "Tier calculation complete",
            ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            detailUrl,
            AdditionalInformation(calculationId),
            PersonReference(listOf(PersonReferenceType("CRN", crn))),
        )
        val event = PublishRequest.builder().topicArn(calculationCompleteTopic.arn)
            .message(objectMapper.writeValueAsString(message))
            .messageAttributes(
                mapOf(
                    "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(eventType).build()
                )
            ).build()
        calculationCompleteTopic.snsClient.publish(event)
    }
}

data class TierChangeEvent(
    val eventType: String,
    val version: Int,
    val description: String,
    val occurredAt: String,
    val detailUrl: String,
    val additionalInformation: AdditionalInformation,
    val personReference: PersonReference,
)

data class AdditionalInformation(
    val calculationId: UUID,
)

data class PersonReference(
    val identifiers: List<PersonReferenceType>,
)

data class PersonReferenceType(
    val type: String,
    val value: String,
)
