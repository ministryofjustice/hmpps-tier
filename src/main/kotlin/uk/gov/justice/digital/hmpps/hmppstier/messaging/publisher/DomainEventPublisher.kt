package uk.gov.justice.digital.hmpps.hmppstier.messaging.publisher

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppstier.messaging.consumer.DomainEvent
import uk.gov.justice.digital.hmpps.hmppstier.messaging.publisher.TierCalculationDomainEvent.AdditionalInformation
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingTopicException
import java.util.*

@Service
class DomainEventPublisher(
    hmppsQueueService: HmppsQueueService,
    private val objectMapper: ObjectMapper,
    @Value("\${hmpps.tier.endpoint.url}") private val hmppsTierEndpointUrl: String,
) {

    private val calculationCompleteTopic = hmppsQueueService.findByTopicId("hmppscalculationcompletetopic")
        ?: throw MissingTopicException("Could not find topic hmppscalculationcompletetopic")

    fun update(crn: String, calculationId: UUID) {
        val message = TierCalculationDomainEvent(
            detailUrl = "$hmppsTierEndpointUrl/crn/$crn/tier/$calculationId",
            additionalInformation = AdditionalInformation(calculationId),
            personReference = DomainEvent.PersonReference(listOf(DomainEvent.Identifier("CRN", crn))),
        )
        val request = PublishRequest.builder()
            .topicArn(calculationCompleteTopic.arn)
            .message(objectMapper.writeValueAsString(message))
            .messageAttributes(
                mapOf(
                    "eventType" to
                        MessageAttributeValue.builder().dataType("String").stringValue(message.eventType).build()
                )
            ).build()
        calculationCompleteTopic.snsClient.publish(request)
    }
}
