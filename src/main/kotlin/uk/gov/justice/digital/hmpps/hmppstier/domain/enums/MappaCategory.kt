package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

enum class MappaCategory(val categoryCode: String) {
    M1("M1"),
    M2("M2"),
    M3("M3"),
    M4("M4"),
    ;

    companion object {
        fun from(categoryCode: String) = entries.firstOrNull { it.categoryCode == categoryCode }
    }
}
