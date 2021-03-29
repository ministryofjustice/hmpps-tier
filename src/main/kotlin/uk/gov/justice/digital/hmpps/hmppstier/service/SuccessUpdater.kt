package uk.gov.justice.digital.hmpps.hmppstier.service

import com.amazonaws.services.sns.AmazonSNSAsync
import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
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
    val event = PublishRequest(topic, gson.toJson(TierChangeEvent(crn, calculationId)))
    with(event) {
      val messageAttributeValue = MessageAttributeValue()
      with(messageAttributeValue) {
        stringValue = EventType.TIER_CALCULATION_COMPLETE.toString()
        dataType = "String"
      }
      addMessageAttributesEntry("eventType", messageAttributeValue)
    }

    amazonSNS.publish(event)
  }
}

private data class TierChangeEvent(
  val crn: String,
  val calculationId: UUID
)

private enum class EventType {
  TIER_CALCULATION_COMPLETE,
}
