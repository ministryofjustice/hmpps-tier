package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusApiClient
import uk.gov.justice.digital.hmpps.hmppstier.domain.RecalculationSource
import uk.gov.justice.digital.hmpps.hmppstier.messaging.consumer.DomainEvent
import uk.gov.justice.digital.hmpps.hmppstier.messaging.consumer.DomainEvent.Identifier
import uk.gov.justice.digital.hmpps.hmppstier.messaging.consumer.DomainEvent.PersonReference
import uk.gov.justice.digital.hmpps.hmppstier.messaging.consumer.SQSMessage
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException

@Service
class RecalculationService(
    hmppsQueueService: HmppsQueueService,
    private val objectMapper: ObjectMapper,
    private val deliusApiClient: DeliusApiClient,
) {

    private val queueUrl = hmppsQueueService.findByQueueId("hmppsdomaineventsqueue")?.queueUrl
        ?: throw MissingQueueException("HmppsQueue hmppsdomaineventsqueue not found")

    private val sqsClient = hmppsQueueService.findByQueueId("hmppsdomaineventsqueue")!!.sqsClient

    fun recalculateAll() {
        deliusApiClient.getActiveCrns()
            .forEach { crn ->
                publishToQueue(crn, RecalculationSource.FullRecalculation)
            }
    }

    fun recalculate(crns: Collection<String>) = crns.forEach { crn ->
        publishToQueue(crn, RecalculationSource.LimitedRecalculation)
    }

    private fun publishToQueue(crn: String, recalculationSource: RecalculationSource) {
        val eventType = "internal.recalculate-tier"
        val domainEvent = DomainEvent(
            eventType = eventType,
            description = "Automated tier re-calculation",
            personReference = PersonReference(listOf(Identifier("CRN", crn))),
            additionalInformation = mapOf(
                "recalculationSource" to recalculationSource::class.java.simpleName
            )
        )
        val messageBody = SQSMessage(objectMapper.writeValueAsString(domainEvent))
        log.info("publishing event type {} for crn {}", eventType, crn)
        sqsClient.sendMessage(
            SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(objectMapper.writeValueAsString(messageBody))
                .messageAttributes(
                    mapOf(
                        "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(eventType)
                            .build(),
                    ),
                ).build()
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
