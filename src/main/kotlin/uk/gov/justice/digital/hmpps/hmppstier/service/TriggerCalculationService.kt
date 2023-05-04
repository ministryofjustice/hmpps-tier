package uk.gov.justice.digital.hmpps.hmppstier.service

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppstier.controller.SQSMessage
import uk.gov.justice.digital.hmpps.hmppstier.controller.TierCalculationMessage
import uk.gov.justice.digital.hmpps.hmppstier.controller.TriggerCsv
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException

@Service
class TriggerCalculationService(
  hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {

  private val hmppsOffenderQueueUrl = hmppsQueueService.findByQueueId("hmppsoffenderqueue")?.queueUrl ?: throw MissingQueueException("HmppsQueue hmppsoffenderqueue not found")

  private val hmppsOffenderSqsClient = hmppsQueueService.findByQueueId("hmppsoffenderqueue")!!.sqsClient
  suspend fun sendEvents(crns: List<TriggerCsv>) {
    CoroutineScope(Dispatchers.IO).launch {
      crns.forEach { crn ->
        publishToHMPPSOffenderQueue(crn)
      }
    }
  }

  private fun publishToHMPPSOffenderQueue(crn: TriggerCsv) {
    val sendMessage = SendMessageRequest.builder().queueUrl(
      hmppsOffenderQueueUrl,
    ).messageBody(
      objectMapper.writeValueAsString(
        crnToOffenderSqsMessage(crn),
      ),
    ).messageAttributes(
      mapOf("eventType" to MessageAttributeValue.builder().dataType("String").stringValue("OFFENDER_MANAGEMENT_TIER_CALCULATION_REQUIRED").build()),
    ).build()
    log.info("publishing event type {} for crn {}", "OFFENDER_MANAGEMENT_TIER_CALCULATION_REQUIRED", crn.crn)
    hmppsOffenderSqsClient.sendMessage(sendMessage)
  }

  private fun crnToOffenderSqsMessage(crn: TriggerCsv): SQSMessage = SQSMessage(
    objectMapper.writeValueAsString(
      TierCalculationMessage(crn.crn!!),
    ),
  )

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
