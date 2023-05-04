package uk.gov.justice.digital.hmpps.hmppstier.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService

@Service
class DomainEventsListener(
  private val calculator: TierCalculationService,
  private val objectMapper: ObjectMapper,
) {

  @SqsListener("hmppsdomaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun listen(msg: String) {
    CoroutineScope(Dispatchers.Default).future {
      calculator.calculateTierForCrn(getCrn(msg), "DomainEventsListener")
    }.get()
  }

  private fun getCrn(msg: String): String {
    val (message) = objectMapper.readValue(msg, SQSMessage::class.java)
    return objectMapper.readValue(message, DomainEventsMessage::class.java)
      .personReference.identifiers.first { it.type == "CRN" }.value
  }
}

data class DomainEventsMessage(
  val personReference: PersonReference,
)

data class PersonReference(
  val identifiers: List<Identifiers>,
)

data class Identifiers(
  val type: String,
  val value: String,
)
