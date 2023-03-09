package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

private val mappaRelatedTypeCodes = setOf("MAPP", "M1", "M2", "M3")
enum class Mappa(val registerCode: String) {
  M1("M1"),
  M2("M2"),
  M3("M3");

  companion object {
    fun from(value: String?, typeCode: String?): Mappa? {
      return if (!mappaRelatedTypeCodes.contains(typeCode)) null else values()
        .firstOrNull { code -> code.registerCode.equals(value, true) }
    }
  }
}
