package uk.gov.justice.digital.hmpps.hmppstier.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppstier.controller.DomainEventsMessage
import uk.gov.justice.digital.hmpps.hmppstier.controller.Identifiers
import uk.gov.justice.digital.hmpps.hmppstier.controller.PersonReference
import uk.gov.justice.digital.hmpps.hmppstier.controller.SQSMessage
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException

@Service
class TriggerCalculationService(
    hmppsQueueService: HmppsQueueService,
    private val objectMapper: ObjectMapper,
    private val tierToDeliusApiClient: TierToDeliusApiClient,
) {

    private val queueUrl = hmppsQueueService.findByQueueId("hmppsdomaineventsqueue")?.queueUrl
        ?: throw MissingQueueException("HmppsQueue hmppsdomaineventsqueue not found")

    private val sqsClient = hmppsQueueService.findByQueueId("hmppsdomaineventsqueue")!!.sqsClient

    fun recalculateAll(dryRun: Boolean) {
        tierToDeliusApiClient.getActiveCrns()
            .forEach { crn ->
                publishToQueue(crn, RecalculationSource.FullRecalculation, dryRun)
            }
    }

    fun recalculate(crns: Collection<String>, dryRun: Boolean) = crns.forEach { crn ->
        publishToQueue(crn, RecalculationSource.LimitedRecalculation, dryRun)
    }

    private fun publishToQueue(crn: String, recalculationSource: RecalculationSource, dryRun: Boolean) {
        val eventType = "internal.recalculate-tier"
        val domainEvent = DomainEventsMessage(
            eventType = eventType,
            personReference = PersonReference(listOf(Identifiers("CRN", crn))),
            additionalInformation = mapOf(
                "dryRun" to dryRun,
                "recalculationSource" to recalculationSource::class.java.simpleName
            )
        )
        val messageBody = SQSMessage(objectMapper.writeValueAsString(domainEvent))
        log.info("publishing event type {} for crn {}", eventType, crn)
        sqsClient.sendMessage(SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(objectMapper.writeValueAsString(messageBody))
            .messageAttributes(
                mapOf(
                    "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(eventType).build(),
                ),
            ).build())
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
