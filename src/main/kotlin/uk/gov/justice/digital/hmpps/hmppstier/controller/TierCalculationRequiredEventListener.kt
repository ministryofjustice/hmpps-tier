package uk.gov.justice.digital.hmpps.hmppstier.controller

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.slf4j.LoggerFactory
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService
import java.time.LocalDateTime

@Service
class TierCalculationRequiredEventListener(
  private val calculator: TierCalculationService,
  private val gson: Gson
) {

  @MessageExceptionHandler()
  fun errorHandler(e: Exception, msg: String) {
    log.info("Failed to calculate tier for CRN ${getMessage(msg)} with error: ${e.message}")
    throw e
  }

  @SqsListener(value = ["\${offender-events.sqs-queue}"], deletionPolicy = ON_SUCCESS)
  fun listen(msg: String) {
    with(getMessage(msg)) {
      calculator.calculateTierForCrn(crn, eventDatetime)
    }
  }

  private fun getMessage(msg: String): TierCalculationMessage {
    val message = gson.fromJson(msg, SQSMessage::class.java).message
    return gson.fromJson(message, TierCalculationMessage::class.java)
      .also { log.info("Tier calculation message decoded for $it") }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

private data class TierCalculationMessage(
  val crn: String,
  val eventDatetime: LocalDateTime
)

private data class SQSMessage(
  @SerializedName("Message") val message: String
)
