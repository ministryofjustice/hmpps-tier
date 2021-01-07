package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

enum class Rosh(val registerCode: String) {
  VERY_HIGH("RVHR"),
  HIGH("RHRH"),
  MEDIUM("RMRH");

  companion object {
    fun from(value: String): Rosh? {
      return values()
        .firstOrNull { code -> code.registerCode.equals(value, true) }
    }
  }
}
