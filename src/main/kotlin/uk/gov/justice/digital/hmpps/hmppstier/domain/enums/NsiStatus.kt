package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

enum class NsiStatus(val value: String) {
  BRE08("BRE08"),
  BRE15("BRE15"),
  BRE16("BRE16"),
  BRE09("BRE09"),
  BRE24("BRE24"),
  BRE25("BRE25");

  companion object {
    fun from(value: String?): NsiStatus? {
      return values()
        .firstOrNull { code -> code.value.equals(value, true) }
    }
  }
}
