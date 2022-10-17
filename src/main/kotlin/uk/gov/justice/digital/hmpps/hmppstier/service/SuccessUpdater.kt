package uk.gov.justice.digital.hmpps.hmppstier.service

import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.service.EventType.TIER_CALCULATION_COMPLETE
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingTopicException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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
    val message = TierChangeEvent(
      crn,
      calculationId,
      "tier.calculation.complete",
      2,
      "Tier calculation complete",
      ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
      detailUrl,
      AdditionalInformation(calculationId),
      PersonReference(listOf(PersonReferenceType("CRN", crn)))
    )
    val event = PublishRequest(calculationCompleteTopic.arn, gson.toJson(message))
    event.withMessageAttributes(mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue(TIER_CALCULATION_COMPLETE.toString())))
    calculationCompleteTopic.snsClient.publish(event)
  }
}

data class TierChangeEvent(
  val crn: String,
  val calculationId: UUID,
  val eventType: String,
  val version: Int,
  val description: String,
  val occurredAt: String,
  val detailUrl: String,
  val additionalInformation: AdditionalInformation,
  val personReference: PersonReference
)

data class AdditionalInformation(
  val calculationId: UUID
)

data class PersonReference(
  val identifiers: List<PersonReferenceType>
)

data class PersonReferenceType(
  val type: String,
  val value: String
)

private enum class EventType {
  TIER_CALCULATION_COMPLETE,
}
