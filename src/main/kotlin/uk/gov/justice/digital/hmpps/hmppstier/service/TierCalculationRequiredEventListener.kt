package uk.gov.justice.digital.hmpps.hmppstier.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener
import org.springframework.stereotype.Service

@Service
class TierCalculationRequiredEventListener(val objectMapper: ObjectMapper, val calculator: TierCalculationService) {

  @SqsListener(value = ["\${offender-events.sqs-queue}"], deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
  fun listen(msg: String) {
    val message: String = objectMapper.readTree(msg)["Message"].asText()
    val typeReference = object : TypeReference<TierCalculationMessage>() {}

    val calculationMessage: TierCalculationMessage = objectMapper.readValue(message, typeReference)
    calculator.calculateTierForCrn(calculationMessage.crn)
  }
}

data class TierCalculationMessage(val crn: String)
