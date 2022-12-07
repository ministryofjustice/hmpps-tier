package uk.gov.justice.digital.hmpps.hmppstier.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService

@Service
class DomainEventsListener(
  private val calculator: TierCalculationService,
  private val objectMapper: ObjectMapper,
) {

  @MessageExceptionHandler()
  fun errorHandler(e: Exception, msg: String) {
    log.warn("Failed to calculate tier for CRN ${getCrn(msg)} with error: ${e.message}")
    throw e
  }

  @JmsListener(destination = "hmppsdomaineventsqueue", containerFactory = "hmppsQueueContainerFactoryProxy")
  fun listen(msg: String) {
    calculator.calculateTierForCrn(getCrn(msg))
  }

  private fun getCrn(msg: String): String {
    val (message) = objectMapper.readValue(msg, SQSMessage::class.java)
    return objectMapper.readValue(message, DomainEventsMessage::class.java)
      .personReference.identifiers.first { it.type == "CRN" }.value
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class DomainEventsMessage(
  val eventType: String,
  val version: Int,
  val description: String,
  val detailUrl: String,
  val occurredAt: String,
  val additionalInformation: AdditionalInformation,
  val personReference: PersonReference,
)

data class AdditionalInformation(
  val eventId: String,
  val outcome: String
)

data class PersonReference(
  val identifiers: List<Identifiers>
)

data class Identifiers(
  val type: String,
  val value: String
)
