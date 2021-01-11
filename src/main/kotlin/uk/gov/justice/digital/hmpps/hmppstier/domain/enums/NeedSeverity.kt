package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

enum class NeedSeverity(val score: Int) {
  NO_NEED(0),
  STANDARD(1),
  SEVERE(2),
}
