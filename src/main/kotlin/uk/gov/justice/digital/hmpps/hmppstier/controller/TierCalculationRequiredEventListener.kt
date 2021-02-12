package uk.gov.justice.digital.hmpps.hmppstier.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.dto.CalculationResultDto
import uk.gov.justice.digital.hmpps.hmppstier.dto.TierDto

@Service
class TierCalculationRequiredEventListener(
  val objectMapper: ObjectMapper,
  val calculator: TierCalculationService,
  val successUpdater: SuccessUpdater
) {

  @SqsListener(value = ["\${offender-events.sqs-queue}"], deletionPolicy = ON_SUCCESS)
  fun listen(msg: String) {
    val message: String = objectMapper.readTree(msg)["Message"].asText()
    val typeReference = object : TypeReference<TierCalculationMessage>() {}

    val calculationMessage: TierCalculationMessage = objectMapper.readValue(message, typeReference)
    val existingCalculation = calculator.getTierCalculation(calculationMessage.crn)
    val tier = calculator.calculateTierForCrn(calculationMessage.crn)
    println("new tier is $tier")

    if (tierHasChanged(tier, existingCalculation)) {
      successUpdater.update(tier, calculationMessage.crn)
    }
  }

  private fun tierHasChanged(
    tier: TierDto,
    existingCalculation: CalculationResultDto
  ) = tier != existingCalculation.tierDto
}

data class TierCalculationMessage(val crn: String)
