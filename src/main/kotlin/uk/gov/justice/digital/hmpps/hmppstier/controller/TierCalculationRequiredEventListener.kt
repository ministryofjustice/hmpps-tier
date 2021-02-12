package uk.gov.justice.digital.hmpps.hmppstier.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.service.SuccessUpdater
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService

@Service
class TierCalculationRequiredEventListener(
  val objectMapper: ObjectMapper,
  val calculator: TierCalculationService,
  val successUpdater: SuccessUpdater
) {

  @SqsListener(value = ["\${offender-events.sqs-queue}"], deletionPolicy = ON_SUCCESS)
  fun listen(msg: String) {
    val crn = getCrn(msg)
    val tier = calculator.calculateTierForCrn(crn)

    if (tier.isUpdated) {
      successUpdater.update(tier.tierDto, crn)
    }
  }

  private fun getCrn(msg: String): String {
    val message: String = objectMapper.readTree(msg)["Message"].asText()
    val typeReference = object : TypeReference<TierCalculationMessage>() {}

    return objectMapper.readValue(message, typeReference).crn
  }
}

data class TierCalculationMessage(val crn: String)
