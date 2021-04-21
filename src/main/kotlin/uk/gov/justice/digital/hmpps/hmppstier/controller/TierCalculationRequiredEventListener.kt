package uk.gov.justice.digital.hmpps.hmppstier.controller

import com.google.gson.Gson
import org.slf4j.LoggerFactory
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener
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
    log.info("Failed to calculate tier for CRN ${getCrn(msg)} with error: ${e.message}")
    throw e
  }

  @SqsListener(value = ["\${offender-events.sqs-queue}"], deletionPolicy = ON_SUCCESS)
  fun listen(msg: String) {
    calculator.calculateTierForCrn(getCrn(msg))
  }

  private fun getCrn(msg: String): String {
    val message = gson.fromJson(msg, SQSMessage::class.java).Message
    return gson.fromJson(message, TierCalculationMessage::class.java).crn
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

private data class TierCalculationMessage(val crn: String)

private data class SQSMessage(
  val Message: String,
  val MessageId: String
)
