package uk.gov.justice.digital.hmpps.hmppstier.service

import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import com.google.gson.Gson
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.service.EventType.TIER_CALCULATION_COMPLETE
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingTopicException
import java.util.UUID

@Service
class SuccessUpdater(
  hmppsQueueService: HmppsQueueService,
  private val gson: Gson
) {

  private val calculationCompleteTopic = hmppsQueueService.findByTopicId("hmppscalculationcompletetopic") ?: throw MissingTopicException("Could not find topic hmppscalculationcompletetopic")

  fun update(crn: String, calculationId: UUID) {
    val event = PublishRequest(calculationCompleteTopic.arn, gson.toJson(TierChangeEvent(crn, calculationId)))
    val messageAttributeValue = MessageAttributeValue()
    with(messageAttributeValue) {
      stringValue = TIER_CALCULATION_COMPLETE.toString()
      dataType = "String"
    }
    event.addMessageAttributesEntry("eventType", messageAttributeValue)
    calculationCompleteTopic.snsClient.publish(event)
  }
}

data class TierChangeEvent(
  val crn: String,
  val calculationId: UUID
)

private enum class EventType {
  TIER_CALCULATION_COMPLETE,
}
