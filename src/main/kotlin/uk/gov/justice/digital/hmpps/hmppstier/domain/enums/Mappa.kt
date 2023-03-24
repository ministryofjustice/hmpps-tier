package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

private const val MAPPA_TYPE_CODE = "MAPP"
enum class Mappa(val registerCode: String) {
  M1("M1"),
  M2("M2"),
  M3("M3"),
  ;

  companion object {
    fun from(value: String?, typeCode: String?): Mappa? {
      return if (typeCode == MAPPA_TYPE_CODE) {
        values()
          .firstOrNull { code -> code.registerCode.equals(value, true) }
      } else {
        null
      }
    }
  }
}
