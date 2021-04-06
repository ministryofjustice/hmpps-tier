package uk.gov.justice.digital.hmpps.hmppstier.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService

@Service
class TierCalculationRequiredEventListener(
  private val objectMapper: ObjectMapper,
  private val calculator: TierCalculationService
) {

  @SqsListener(value = ["\${offender-events.sqs-queue}"], deletionPolicy = ON_SUCCESS)
  fun listen(msg: String) {
    calculator.calculateTierForCrn(getCrn(msg))
  }

  private fun getCrn(msg: String): String {
    val message: String = objectMapper.readTree(msg)["Message"].asText()
    return objectMapper.readValue(message, typeReference).crn
      .also { log.info("Tier calculation message decoded for $it") }
  }

  companion object {
    private val typeReference = object : TypeReference<TierCalculationMessage>() {}
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

private data class TierCalculationMessage(val crn: String)
