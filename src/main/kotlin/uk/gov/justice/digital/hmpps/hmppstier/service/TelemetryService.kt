package uk.gov.justice.digital.hmpps.hmppstier.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.service.TelemetryEventType.TIER_CHANGED
import uk.gov.justice.digital.hmpps.hmppstier.service.TelemetryEventType.TIER_UNCHANGED

@Component
class TelemetryService(@Autowired private val telemetryClient: TelemetryClient) {

  fun trackTierCalculated(calculation: TierCalculationEntity, isUpdated: Boolean) {
    trackEvent(
      if (isUpdated) {
        TIER_CHANGED
      } else {
        TIER_UNCHANGED
      },
      mapOf(
        "crn" to calculation.crn,
        "protect" to calculation.data.protect.tier.value,
        "change" to calculation.data.change.tier.value.toString(),
        "version" to calculation.data.calculationVersion,
      ),
    )
  }

  private fun trackEvent(eventType: TelemetryEventType, customDimensions: Map<String, String?>) {
    telemetryClient.trackEvent(eventType.eventName, customDimensions, null)
  }
}
