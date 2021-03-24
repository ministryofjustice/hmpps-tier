package uk.gov.justice.digital.hmpps.hmppstier.service

import com.amazonaws.services.sns.AmazonSNSAsync
import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SuccessUpdater(
  private val amazonSNS: AmazonSNSAsync,
  @Value("\${hmpps-events.topic}")private val topic: String,
  private val gson: Gson
) {

  fun update(crn: String, calculationId: UUID) {
    val message = Message(EventType.TIER_CALCULATION_COMPLETE, gson.toJson(TierChangeEvent(crn, calculationId)), calculationId.toString())

    amazonSNS.publish(topic, gson.toJson(message))
  }
}

private data class TierChangeEvent(
  val crn: String,
  val calculationId: UUID
)

private data class Message(
  val eventType: EventType,
  val Message: String,
  val MessageId: String
)

private enum class EventType {
  TIER_CALCULATION_COMPLETE,
}
