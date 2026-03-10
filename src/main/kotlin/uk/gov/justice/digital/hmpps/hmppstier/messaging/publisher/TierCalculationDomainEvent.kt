package uk.gov.justice.digital.hmpps.hmppstier.messaging.publisher

import uk.gov.justice.digital.hmpps.hmppstier.messaging.consumer.DomainEvent
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

data class TierCalculationDomainEvent(
    val version: Int = 2,
    val eventType: String = "tier.calculation.complete",
    val description: String = "Tier calculation complete",
    val occurredAt: String = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
    val detailUrl: String,
    val additionalInformation: AdditionalInformation,
    val personReference: DomainEvent.PersonReference,
) {
    data class AdditionalInformation(
        val calculationId: UUID,
    )
}