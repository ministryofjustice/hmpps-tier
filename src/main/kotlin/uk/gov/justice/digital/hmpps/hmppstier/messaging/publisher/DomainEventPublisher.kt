package uk.gov.justice.digital.hmpps.hmppstier.messaging.publisher

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.hmppstier.messaging.consumer.DomainEvent
import uk.gov.justice.digital.hmpps.hmppstier.messaging.consumer.DomainEvent.AdditionalInformation
import uk.gov.justice.digital.hmpps.hmppstier.messaging.consumer.DomainEvent.PersonReference
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingTopicException
import java.util.*

@Service
class DomainEventPublisher(
    hmppsQueueService: HmppsQueueService,
    private val objectMapper: ObjectMapper,
    @Value("\${hmpps.tier.endpoint.url}") private val hmppsTierEndpointUrl: String,
    @Value("\${feature.v3.enabled}") private val v3Enabled: Boolean,
) {

    private val calculationCompleteTopic = hmppsQueueService.findByTopicId("hmppscalculationcompletetopic")
        ?: throw MissingTopicException("Could not find topic hmppscalculationcompletetopic")

    fun update(crn: String, isUpdated: Boolean, calculationId: UUID) {
        publishCalculation(crn, calculationId)
        if (isUpdated) publishChange(crn, calculationId)
    }

    private fun publishCalculation(crn: String, calculationId: UUID) {
        val message = TierCalculationDomainEvent(
            version = if (v3Enabled) 3 else 2,
            detailUrl = "$hmppsTierEndpointUrl/crn/$crn/tier/$calculationId",
            additionalInformation = AdditionalInformation(calculationId),
            personReference = PersonReference(listOf(DomainEvent.Identifier("CRN", crn))),
        )
        val request = PublishRequest.builder()
            .topicArn(calculationCompleteTopic.arn)
            .message(objectMapper.writeValueAsString(message))
            .messageAttributes(attribute(message.eventType)).build()
        calculationCompleteTopic.snsClient.publish(request)
    }

    private fun publishChange(crn: String, calculationId: UUID) {
        val message = TierChangeDomainEvent(
            detailUrl = "$hmppsTierEndpointUrl/crn/$crn/tier/$calculationId",
            additionalInformation = AdditionalInformation(calculationId),
            personReference = PersonReference(listOf(DomainEvent.Identifier("CRN", crn))),
        )
        val request = PublishRequest.builder()
            .topicArn(calculationCompleteTopic.arn)
            .message(objectMapper.writeValueAsString(message))
            .messageAttributes(attribute(message.eventType)).build()
        calculationCompleteTopic.snsClient.publish(request)
    }

    private fun attribute(eventType: String) = mapOf(
        "eventType" to MessageAttributeValue.builder()
            .dataType("String")
            .stringValue(eventType)
            .build()
    )
}
