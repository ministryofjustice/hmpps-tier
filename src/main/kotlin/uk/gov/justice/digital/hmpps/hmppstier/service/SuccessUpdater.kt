package uk.gov.justice.digital.hmpps.hmppstier.service

import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.service.EventType.TIER_CALCULATION_COMPLETE
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingTopicException
import java.util.UUID

@Service
class SuccessUpdater(
  hmppsQueueService: HmppsQueueService,
  private val gson: Gson,
  @Value("\${hmpps.tier.endpoint.url}") private val hmppsTierEndpointUrl: String,
) {

  private val calculationCompleteTopic = hmppsQueueService.findByTopicId("hmppscalculationcompletetopic") ?: throw MissingTopicException("Could not find topic hmppscalculationcompletetopic")

  fun update(crn: String, calculationId: UUID) {
    val detailUrl = "$hmppsTierEndpointUrl/crn/$crn/tier/$calculationId"
    val event = PublishRequest(calculationCompleteTopic.arn, gson.toJson(TierChangeEvent(crn, calculationId, detailUrl)))
    event.withMessageAttributes(mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue(TIER_CALCULATION_COMPLETE.toString())))
    calculationCompleteTopic.snsClient.publish(event)
  }
}

data class TierChangeEvent(
  val crn: String,
  val calculationId: UUID,
  val detailUrl: String
)

private enum class EventType {
  TIER_CALCULATION_COMPLETE,
}
