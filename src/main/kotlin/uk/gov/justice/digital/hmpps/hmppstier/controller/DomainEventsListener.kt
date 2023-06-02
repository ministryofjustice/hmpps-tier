package uk.gov.justice.digital.hmpps.hmppstier.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService
import java.util.concurrent.CompletableFuture

@Service
class DomainEventsListener(
  private val calculator: TierCalculationService,
  private val objectMapper: ObjectMapper,
) {

  @SqsListener("hmppsdomaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun listen(msg: String): CompletableFuture<Void> {
    return CoroutineScope(Dispatchers.Default).future {
      calculator.calculateTierForCrn(getCrn(msg), "DomainEventsListener")
    }.thenAccept {}
  }

  private fun getCrn(msg: String): String {
    val (message) = objectMapper.readValue(msg, SQSMessage::class.java)
    val domainEventMessage = objectMapper.readValue(message, DomainEventsMessage::class.java)
    val crn = domainEventMessage.personReference.identifiers.first { it.type == "CRN" }.value
    log.info("Domain event received of type ${domainEventMessage.eventType} and CRN: $crn")
    return crn
  }

  companion object {
    private val log =
      LoggerFactory.getLogger(this::class.java)
  }
}

data class DomainEventsMessage(
  val eventType: String,
  val personReference: PersonReference,
)

data class PersonReference(
  val identifiers: List<Identifiers>,
)

data class Identifiers(
  val type: String,
  val value: String,
)
