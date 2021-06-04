package uk.gov.justice.digital.hmpps.hmppstier.integration.bdd

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.google.gson.Gson
import io.cucumber.java8.En
import org.assertj.core.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.SQSMessage
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.oneMessageCurrentlyOnQueue
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import uk.gov.justice.digital.hmpps.hmppstier.service.TierChangeEvent

class ThenSteps : En {
  @Autowired
  lateinit var gson: Gson

  @Autowired
  lateinit var calculationCompleteClient: AmazonSQSAsync

  @Value("\${calculation-complete.sqs-queue}")
  lateinit var calculationCompleteUrl: String

  @Autowired
  lateinit var tierCalculationRepository: TierCalculationRepository

  init {
    Then("{int} protect points are scored") { points: Int ->
      val calculation: TierCalculationEntity = getTier()
      Assertions.assertThat(calculation.data.protect.points).isEqualTo(points)
    }

    Then("{int} change points are scored") { points: Int ->
      val calculation: TierCalculationEntity = getTier()
      Assertions.assertThat(calculation.data.change.points).isEqualTo(points)
    }

    Then("there is a mandate for change") {
      val calculation: TierCalculationEntity = getTier()
      Assertions.assertThat(calculation.data.change.tier.value).isEqualTo(1)
    }

    Then("a change level of {int} is returned and {int} points are scored") { changeLevel: Int, points: Int ->
      changeIs(changeLevel, points)
    }

    Then("a protect level of {string} is returned and {int} points are scored") { protectLevel: String, points: Int ->
      val calculation: TierCalculationEntity = getTier()
      Assertions.assertThat(calculation.data.protect.tier).isEqualTo(ProtectLevel.valueOf(protectLevel))
      Assertions.assertThat(calculation.data.protect.points).isEqualTo(points)
    }

    Then("there is no mandate for change") {
      changeIs(0, 0)
    }
  }

  private fun changeIs(tier: Int, points: Int) {
    val calculation: TierCalculationEntity = getTier()
    Assertions.assertThat(calculation.data.change.tier.value).isEqualTo(tier)
    Assertions.assertThat(calculation.data.change.points).isEqualTo(points)
  }

  private fun getTier(): TierCalculationEntity {
    oneMessageCurrentlyOnQueue(calculationCompleteClient, calculationCompleteUrl)
    val message = calculationCompleteClient.receiveMessage(calculationCompleteUrl)
    val sqsMessage: SQSMessage = gson.fromJson(message.messages[0].body, SQSMessage::class.java)
    val changeEvent: TierChangeEvent = gson.fromJson(sqsMessage.Message, TierChangeEvent::class.java)

    return tierCalculationRepository.findByCrnAndUuid(changeEvent.crn, changeEvent.calculationId)!!
  }
}
