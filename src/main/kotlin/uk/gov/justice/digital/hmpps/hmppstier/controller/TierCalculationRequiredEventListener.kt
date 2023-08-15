package uk.gov.justice.digital.hmpps.hmppstier.controller

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService

@Service
class TierCalculationRequiredEventListener(
  private val calculator: TierCalculationService,
  private val objectMapper: ObjectMapper,
) {

  @MessageExceptionHandler()
  fun errorHandler(e: Exception, msg: String) {
    log.warn("Failed to calculate tier for CRN ${getCrn(msg).crn} with error: ${e.message}")
    throw e
  }

  @SqsListener("hmppsoffenderqueue", factory = "hmppsQueueContainerFactoryProxy")
  suspend fun listen(msg: String) {
    val (crn) = getCrn(msg)
    calculator.calculateTierForCrn(crn, "TierCalculationRequiredEventListener")
  }

  private fun getCrn(msg: String): TierCalculationMessage {
    val (message) = objectMapper.readValue(msg, SQSMessage::class.java)
    return objectMapper.readValue(message, TierCalculationMessage::class.java)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class TierCalculationMessage(val crn: String)

data class SQSMessage(@JsonProperty("Message") val message: String)
