package uk.gov.justice.digital.hmpps.hmppstier.controller

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.service.RecalculationSource
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService

@Service
class TierCalculationRequiredEventListener(
  private val calculator: TierCalculationService,
  private val objectMapper: ObjectMapper,
) {
  @MessageExceptionHandler
  fun errorHandler(e: Exception, msg: String) {
    log.warn("Failed to calculate tier for CRN ${getRecalculation(msg).crn} with error: ${e.message}")
    throw e
  }

  @SqsListener("hmppsoffenderqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun listen(msg: String) = runBlocking {
    val recalculation = getRecalculation(msg)
    calculator.calculateTierForCrn(recalculation.crn, recalculation.recalculationSource ?: RecalculationSource.OffenderEventRecalculation)
  }

  private fun getRecalculation(msg: String): TierCalculationMessage {
    val (message) = objectMapper.readValue<SQSMessage>(msg)
    return objectMapper.readValue<TierCalculationMessage>(message)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class TierCalculationMessage(val crn: String, val recalculationSource: RecalculationSource? = null)

data class SQSMessage(@JsonProperty("Message") val message: String)
