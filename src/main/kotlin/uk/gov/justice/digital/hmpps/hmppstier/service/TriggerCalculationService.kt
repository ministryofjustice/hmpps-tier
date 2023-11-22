package uk.gov.justice.digital.hmpps.hmppstier.service

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppstier.controller.SQSMessage
import uk.gov.justice.digital.hmpps.hmppstier.controller.TierCalculationMessage
import uk.gov.justice.digital.hmpps.hmppstier.controller.TriggerCsv
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

@Service
class TriggerCalculationService(
  hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
  private val tierToDeliusApiClient: TierToDeliusApiClient,
  private val tierCalculationService: TierCalculationService,
) {
  private val recalculationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val semaphore = Semaphore(16)

  private val hmppsOffenderQueueUrl = hmppsQueueService.findByQueueId("hmppsoffenderqueue")?.queueUrl
    ?: throw MissingQueueException("HmppsQueue hmppsoffenderqueue not found")

  private val hmppsOffenderSqsClient = hmppsQueueService.findByQueueId("hmppsoffenderqueue")!!.sqsClient
  suspend fun sendEvents(crns: List<TriggerCsv>) {
    CoroutineScope(Dispatchers.IO).launch {
      crns.forEach { csv ->
        csv.crn?.let {
          publishToHMPPSOffenderQueue(it)
        }
      }
    }
  }

  suspend fun recalculateAll() {
    val start = LocalDateTime.now()
    log.info("Starting full recalculation request: $start")
    val received = AtomicLong(0)
    val processed = AtomicLong(0)
    tierToDeliusApiClient.getActiveCrns()
      .buffer()
      .collect {
        log.debug("Full Recalculation Received: ${received.incrementAndGet()}")
        semaphore.withPermit {
          recalculationScope.launch {
            tierCalculationService.calculateTierForCrn(it, RecalculationSource.FullRecalculation)
            log.debug("Full Recalculation Processed: ${processed.incrementAndGet()}")
          }
        }
      }
    val end = LocalDateTime.now()
    log.info("Full recalculation Completed - took ${Duration.between(start, end)}")
  }

  suspend fun recalculate(crns: Flow<String>) = crns.collect {
    semaphore.withPermit {
      recalculationScope.launch {
        tierCalculationService.calculateTierForCrn(it, RecalculationSource.LimitedRecalculation)
      }
    }
  }

  private fun publishToHMPPSOffenderQueue(crn: String) {
    val sendMessage = SendMessageRequest.builder().queueUrl(
      hmppsOffenderQueueUrl,
    ).messageBody(
      objectMapper.writeValueAsString(
        crnToOffenderSqsMessage(crn),
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

  private fun crnToOffenderSqsMessage(crn: String): SQSMessage = SQSMessage(
    objectMapper.writeValueAsString(
      TierCalculationMessage(crn),
    ),
  )

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
