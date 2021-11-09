package uk.gov.justice.digital.hmpps.hmppstier.controller

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService

@Service
class TierCalculationRequiredEventListener(
  private val calculator: TierCalculationService,
  private val gson: Gson
) {

  @MessageExceptionHandler()
  fun errorHandler(e: Exception, msg: String) {
    log.warn("Failed to calculate tier for CRN ${getCrn(msg)} with error: ${e.message}")
    throw e
  }

  @JmsListener(destination = "hmppsoffenderqueue", containerFactory = "hmppsQueueContainerFactoryProxy")
  fun listen(msg: String) {
    calculator.calculateTierForCrn(getCrn(msg))
  }

  private fun getCrn(msg: String): String {
    val message = gson.fromJson(msg, SQSMessage::class.java).message
    return gson.fromJson(message, TierCalculationMessage::class.java).crn
      .also { log.debug("Tier calculation message decoded for $it") }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

private data class TierCalculationMessage(val crn: String)

private data class SQSMessage(@SerializedName("Message") val message: String)
