package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

enum class AssessmentComplexityFactor(val section: String, val answerCode: String) {

  IMPULSIVITY("11", "11.2"),
  TEMPER_CONTROL("11", "11.4"),
  PARENTING_RESPONSIBILITIES("13", "13.3 - F");

  companion object {
    fun from(value: String?): AssessmentComplexityFactor? {
      return values()
        .firstOrNull { code -> code.answerCode.equals(value, true) }
    }
  }
}
