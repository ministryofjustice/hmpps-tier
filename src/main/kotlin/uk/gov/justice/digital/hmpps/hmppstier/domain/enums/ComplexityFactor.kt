package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

enum class ComplexityFactor(val registerCode: String) {

  MENTAL_HEALTH("RMDO"),
  ATTEMPTED_SUICIDE_OR_SELF_HARM("ALSH"),
  VULNERABILITY_ISSUE("RVLN"),
  CHILD_CONCERNS("RCCO"),
  CHILD_PROTECTION("RCPR"),
  RISK_TO_CHILDREN("RCHD"),
  PUBLIC_INTEREST("RPIR"),
  ADULT_AT_RISK("RVAD"),
  STREET_GANGS("STRG"),
  TERRORISM("RTAO");

  companion object {
    fun from(value: String?): ComplexityFactor? {
      return values()
        .firstOrNull { code -> code.registerCode.equals(value, true) }
    }
  }
}
