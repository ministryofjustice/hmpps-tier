package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

enum class ComplexityFactor(val registerCode: String) {

  IOM_NOMINAL("IIOM"),
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

  // TODO: This is more complicated
  //BREACH_OR_RECALL

  companion object {
    fun from(value: String): ComplexityFactor? {
      return values()
        .firstOrNull { code -> code.registerCode.equals(value, true) }
    }
  }
}