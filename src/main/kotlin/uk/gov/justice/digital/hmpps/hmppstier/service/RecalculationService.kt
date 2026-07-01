package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry
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
        deliusApiClient.getActiveCrns().chunked(10)
            .forEach { crns -> publishBatch(crns, RecalculationSource.FullRecalculation) }
    }

    fun recalculate(crns: Collection<String>) {
        crns.chunked(10).forEach { crns -> publishBatch(crns, RecalculationSource.LimitedRecalculation) }
    }

    private fun publishBatch(
        crns: List<String>,
        recalculationSource: RecalculationSource
    ) {
        val eventType = "internal.recalculate-tier"
        log.info("publishing event type {} for crns {}", eventType, crns)
        sqsClient.sendMessageBatch(
            SendMessageBatchRequest.builder()
                .queueUrl(queueUrl)
                .entries(crns.mapIndexed { index, crn ->
                    val domainEvent = DomainEvent(
                        eventType = eventType,
                        description = "Automated tier re-calculation",
                        personReference = PersonReference(listOf(Identifier("CRN", crn))),
                        additionalInformation = mapOf(
                            "recalculationSource" to recalculationSource::class.java.simpleName
                        )
                    )
                    val messageBody = SQSMessage(objectMapper.writeValueAsString(domainEvent))
                    SendMessageBatchRequestEntry.builder()
                        .id("$index-$crn")
                        .messageBody(objectMapper.writeValueAsString(messageBody))
                        .messageAttributes(
                            mapOf(
                                "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(eventType)
                                    .build()
                            )
                        )
                        .build()
                })
                .build()
        ).whenComplete { _: Any?, _: Any? ->
            crns.forEach { log.info("published event type {} for crn {}", eventType, it) }
        }.join()
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
