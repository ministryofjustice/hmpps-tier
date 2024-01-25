package uk.gov.justice.digital.hmpps.hmppstier.service

enum class TelemetryEventType(val eventName: String) {
    TIER_CHANGED("TierChanged"),
    TIER_UNCHANGED("TierUnchanged"),
    TIER_CALCULATION_FAILED("TierCalculationFailed"),
    TIER_CALCULATION_REMOVED("TierCalculationRemoved"),
    TIER_CALCULATION_REMOVAL_FAILED("TierCalculationRemovalFailed"),
    TIER_RECALCULATION_DRY_RUN("TierCalculationDryRun"),
    TIER_RECALCULATION_DRY_RUN_FAILURE("TierCalculationDryRunFailure"),
}
