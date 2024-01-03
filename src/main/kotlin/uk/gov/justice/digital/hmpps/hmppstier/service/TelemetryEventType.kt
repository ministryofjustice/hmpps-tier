package uk.gov.justice.digital.hmpps.hmppstier.service

enum class TelemetryEventType(val eventName: String) {
    TIER_CHANGED("TierChanged"),
    TIER_UNCHANGED("TierUnchanged"),
    TIER_CALCULATION_FAILED("TierCalculationFailed"),
    TIER_CALCULATION_REMOVED("TierCalculationRemoved"),
    TIER_CALCULATION_REMOVAL_FAILED("TierCalculationRemovalFailed")
}
