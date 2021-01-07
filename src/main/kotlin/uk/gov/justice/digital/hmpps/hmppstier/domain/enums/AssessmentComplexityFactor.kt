package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

enum class AssessmentComplexityFactor(val answerCode: String) {

  //SELF_CONTROL("0.0"),
  PARENTING_RESPONSIBILITIES("13.3 - F");

  companion object {
    fun from(value: String): AssessmentComplexityFactor? {
      return values()
        .firstOrNull { code -> code.answerCode.equals(value, true) }
    }
  }
}