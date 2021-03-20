package uk.gov.justice.digital.hmpps.hmppstier.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppstier.dto.CalculationResultDto

@Component
class TelemetryService(@Autowired private val telemetryClient: TelemetryClient) {

  private fun trackEvent(eventType: TelemetryEventType, customDimensions: Map<String, String?>) {
    telemetryClient.trackEvent(eventType.eventName, customDimensions, null)
  }

  fun trackTierCalculated(crn: String, calculation: CalculationResultDto) {
    trackEvent(
      if (calculation.isUpdated)
        TelemetryEventType.TIER_CHANGED
      else
        TelemetryEventType.TIER_UNCHANGED,
      mapOf(
        "crn" to crn,
        "protect" to calculation.tierDto.protectLevel.value,
        "change" to calculation.tierDto.changeLevel.value.toString()
      )
    )
  }
}