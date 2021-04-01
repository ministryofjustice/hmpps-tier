package uk.gov.justice.digital.hmpps.hmppstier.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity

@Component
class TelemetryService(@Autowired private val telemetryClient: TelemetryClient) {

  private fun trackEvent(eventType: TelemetryEventType, customDimensions: Map<String, String?>) {
    telemetryClient.trackEvent(eventType.eventName, customDimensions, null)
  }

  fun trackTierCalculated(crn: String, calculation: TierCalculationEntity, isUpdated: Boolean) {
    trackEvent(
      if (isUpdated)
        TelemetryEventType.TIER_CHANGED
      else
        TelemetryEventType.TIER_UNCHANGED,
      mapOf(
        "crn" to crn,
        "protect" to calculation.data.protect.tier.value,
        "change" to calculation.data.change.tier.value.toString(),
        "version" to calculation.data.calculationVersion
      )
    )
  }
}
