package uk.gov.justice.digital.hmpps.hmppstier.messaging.publisher

import uk.gov.justice.digital.hmpps.hmppstier.messaging.consumer.DomainEvent.AdditionalInformation
import uk.gov.justice.digital.hmpps.hmppstier.messaging.consumer.DomainEvent.PersonReference
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class TierChangeDomainEvent(
    val version: Int = 1,
    val eventType: String = "tier.calculation.changed",
    val description: String = "Tier calculation resulted in an updated tier value",
    val occurredAt: String = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
    val detailUrl: String,
    val additionalInformation: AdditionalInformation,
    val personReference: PersonReference,
)