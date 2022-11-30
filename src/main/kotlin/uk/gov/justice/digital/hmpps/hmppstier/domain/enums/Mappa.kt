package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

enum class Mappa(val registerCode: String) {
  M1("M1"),
  M2("M2"),
  M3("M3");

  companion object {
    fun from(value: String?, typeCode: String?): Mappa? {
      if (typeCode != "MAPP") {
        return null
      }
      return values()
        .firstOrNull { code -> code.registerCode.equals(value, true) }
    }
  }
}
