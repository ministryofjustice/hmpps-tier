package uk.gov.justice.digital.hmpps.hmppstier.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.dto.CalculationResultDto
import uk.gov.justice.digital.hmpps.hmppstier.service.SuccessUpdater
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService

@Service
class TierCalculationRequiredEventListener(
  val objectMapper: ObjectMapper,
  val calculator: TierCalculationService,
  val successUpdater: SuccessUpdater,
  @Value("\${flags.enableDeliusTierUpdates}") val enableUpdates: Boolean
) {

  @SqsListener(value = ["\${offender-events.sqs-queue}"], deletionPolicy = ON_SUCCESS)
  fun listen(msg: String) {
    val crn = getCrn(msg)
    val tier = calculator.calculateTierForCrn(crn)

    log.info("Tier calculated for $crn. Different from previous tier: ${tier.isUpdated}. Send update to delius enabled: $enableUpdates")
    if (shouldSendUpdate(tier)) {
      successUpdater.update(tier.tierDto, crn)
    }
  }

  private fun shouldSendUpdate(tier: CalculationResultDto) = enableUpdates && tier.isUpdated

  private fun getCrn(msg: String): String {
    val message: String = objectMapper.readTree(msg)["Message"].asText()
    val typeReference = object : TypeReference<TierCalculationMessage>() {}

    return objectMapper.readValue(message, typeReference).crn
  }

  companion object {
    private val log = LoggerFactory.getLogger(TierCalculationRequiredEventListener::class.java)
  }
}

data class TierCalculationMessage(val crn: String)
