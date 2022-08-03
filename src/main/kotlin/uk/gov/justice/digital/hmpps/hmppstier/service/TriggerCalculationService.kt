package uk.gov.justice.digital.hmpps.hmppstier.service

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.MessageAttributeValue
import com.amazonaws.services.sqs.model.SendMessageRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.controller.SQSMessage
import uk.gov.justice.digital.hmpps.hmppstier.controller.TierCalculationMessage
import uk.gov.justice.digital.hmpps.hmppstier.controller.TriggerCsv
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException

@Service
class TriggerCalculationService(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
  @Qualifier("hmppsoffenderqueue-sqs-client") private val hmppsOffenderSqsClient: AmazonSQSAsync,
) {

  private val hmppsOffenderQueueUrl by lazy { hmppsQueueService.findByQueueId("hmppsoffenderqueue")?.queueUrl ?: throw MissingQueueException("HmppsQueue hmppsoffenderqueue not found") }

  @Async
  fun sendEvents(crns: List<TriggerCsv>) {
    crns.forEach { crn ->
      publishToHMPPSOffenderQueue(crn)
    }
  }

  private fun publishToHMPPSOffenderQueue(crn: TriggerCsv) {
    val sendMessage = SendMessageRequest(
      hmppsOffenderQueueUrl,
      objectMapper.writeValueAsString(
        crnToOffenderSqsMessage(crn.crn!!)
      )
    ).withMessageAttributes(
      mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue("OFFENDER_MANAGEMENT_TIER_CALCULATION_REQUIRED"))
    )
    log.info("publishing event type {} for crn {}", "OFFENDER_MANAGEMENT_TIER_CALCULATION_REQUIRED", crn.crn)
    hmppsOffenderSqsClient.sendMessage(sendMessage)
  }

  private fun crnToOffenderSqsMessage(crn: String): SQSMessage = SQSMessage(
    objectMapper.writeValueAsString(
      TierCalculationMessage(crn)
    )
  )

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
