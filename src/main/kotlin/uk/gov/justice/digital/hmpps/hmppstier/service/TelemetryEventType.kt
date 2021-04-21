package uk.gov.justice.digital.hmpps.hmppstier.service

enum class TelemetryEventType(val eventName: String) {
  TIER_CHANGED("TierChanged"),
  TIER_UNCHANGED("TierUnchanged"),
  NO_ASSESSMENT_RETURNED("NoAssessmentReturned"),
  NO_ASSESSMENT_OF_CORRECT_STATUS("NoAssessmentOfCorrectStatus"),
  NO_ASSESSMENT_IN_DATE("NoAssessmentInDate");
}
