package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

enum class NsiStatus(val value: String) {
  BRE08("BRE08"),
  BRE09("BRE09"),
  BRE15("BRE15"),
  BRE16("BRE16"),
  BRE24("BRE24"),
  BRE25("BRE25"),
  REC01("REC01"),
  REC02("REC02"),
  REC03("REC03"),
  REC04("REC04"),
  REC05("REC05"),
  REC07("REC07"),
  REC08("REC08"),
  REC09("REC09");

  companion object {
    fun from(value: String?): NsiStatus? {
      return values()
        .firstOrNull { code -> code.value.equals(value, true) }
    }
  }
}
