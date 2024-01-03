package uk.gov.justice.digital.hmpps.hmppstier.integration.bdd

import com.fasterxml.jackson.databind.ObjectMapper
import io.cucumber.java8.En
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import uk.gov.justice.digital.hmpps.hmppstier.controller.SQSMessage
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.oneMessageCurrentlyOnQueue
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import uk.gov.justice.digital.hmpps.hmppstier.service.TierChangeEvent
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.util.*

class ThenSteps : En {
  @Autowired
  lateinit var objectMapper: ObjectMapper

  @Qualifier("hmppscalculationcompletequeue-sqs-client")
  @Autowired
  lateinit var calculationCompleteClient: SqsAsyncClient

  private val calculationCompleteUrl by lazy {
    hmppsQueueService.findByQueueId("hmppscalculationcompletequeue")?.queueUrl
      ?: throw MissingQueueException("HmppsQueue tiercalculationqueue not found")
  }

  @Autowired
  private lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  lateinit var tierCalculationRepository: TierCalculationRepository

  init {
    Then("{int} protect points are scored") { points: Int ->
      val calculation: TierCalculationEntity = getTier()
      assertThat(calculation.data.protect.points).isEqualTo(points)
    }

    Then("{int} change points are scored") { points: Int ->
      val calculation: TierCalculationEntity = getTier()
      assertThat(calculation.data.change.points).isEqualTo(points)
    }

    Then("there is a mandate for change") {
      val calculation: TierCalculationEntity = getTier()
      assertThat(calculation.data.change.tier.value).isEqualTo(1)
    }

    Then("a change level of {int} is returned and {int} points are scored") { changeLevel: Int, points: Int ->
      changeIs(changeLevel, points)
    }

    Then("a protect level of {string} is returned and {int} points are scored") { protectLevel: String, points: Int ->
      val calculation: TierCalculationEntity = getTier()
      assertThat(calculation.data.protect.tier).isEqualTo(ProtectLevel.valueOf(protectLevel))
      assertThat(calculation.data.protect.points).isEqualTo(points)
    }

    Then("a protect level of {string} is returned and {int} change points are scored") { protectLevel: String, changeTier: Int ->
      val calculation: TierCalculationEntity = getTier()
      assertThat(calculation.data.protect.tier).isEqualTo(ProtectLevel.valueOf(protectLevel))
      assertThat(calculation.data.change.tier.value).isEqualTo(changeTier)
    }

    Then("there is no mandate for change") {
      changeIs(0, 0)
    }
  }

  private fun changeIs(tier: Int, points: Int) {
    val calculation: TierCalculationEntity = getTier()
    assertThat(calculation.data.change.tier.value).isEqualTo(tier)
    assertThat(calculation.data.change.points).isEqualTo(points)
  }

    private fun getTier(): TierCalculationEntity {
        oneMessageCurrentlyOnQueue(calculationCompleteClient, calculationCompleteUrl)
        val message = calculationCompleteClient.receiveMessage(
            ReceiveMessageRequest.builder().queueUrl(calculationCompleteUrl).build()
        ).get()
        val sqsMessage: SQSMessage = objectMapper.readValue(message.messages()[0].body(), SQSMessage::class.java)
        val changeEvent: TierChangeEvent = objectMapper.readValue(sqsMessage.message, TierChangeEvent::class.java)

    return tierCalculationRepository.findByCrnAndUuid(changeEvent.crn(), changeEvent.calculationId())!!
  }

  private fun TierChangeEvent.crn(): String = this.personReference.identifiers[0].value

  private fun TierChangeEvent.calculationId(): UUID = this.additionalInformation.calculationId
}
