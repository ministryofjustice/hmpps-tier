package uk.gov.justice.digital.hmpps.hmppstier.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppstier.controller.SQSMessage
import uk.gov.justice.digital.hmpps.hmppstier.controller.TierCalculationMessage
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException

@Service
class TriggerCalculationService(
  hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
  private val tierToDeliusApiClient: TierToDeliusApiClient,
) {

  private val hmppsOffenderQueueUrl = hmppsQueueService.findByQueueId("hmppsoffenderqueue")?.queueUrl
    ?: throw MissingQueueException("HmppsQueue hmppsoffenderqueue not found")

  private val hmppsOffenderSqsClient = hmppsQueueService.findByQueueId("hmppsoffenderqueue")!!.sqsClient

  fun recalculateAll() {
    tierToDeliusApiClient.getActiveCrns()
      .forEach { crn ->
        publishToHMPPSOffenderQueue(crn, RecalculationSource.FullRecalculation)
      }
  }

  fun recalculate(crns: List<String>) = crns.forEach { crn ->
    publishToHMPPSOffenderQueue(crn, RecalculationSource.LimitedRecalculation)
  }

  private fun publishToHMPPSOffenderQueue(crn: String, recalculationSource: RecalculationSource) {
    val sendMessage = SendMessageRequest.builder().queueUrl(
      hmppsOffenderQueueUrl,
    ).messageBody(
      objectMapper.writeValueAsString(
        crnToOffenderSqsMessage(crn, recalculationSource),
      ),
    ).messageAttributes(
      mapOf(
        "eventType" to MessageAttributeValue.builder().dataType("String")
          .stringValue("OFFENDER_MANAGEMENT_TIER_CALCULATION_REQUIRED").build(),
      ),
    ).build()
    log.info("publishing event type {} for crn {}", "OFFENDER_MANAGEMENT_TIER_CALCULATION_REQUIRED", crn)
    hmppsOffenderSqsClient.sendMessage(sendMessage)
  }

  private fun crnToOffenderSqsMessage(crn: String, recalculationSource: RecalculationSource): SQSMessage = SQSMessage(
    objectMapper.writeValueAsString(
      TierCalculationMessage(crn, recalculationSource),
    ),
  )

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
