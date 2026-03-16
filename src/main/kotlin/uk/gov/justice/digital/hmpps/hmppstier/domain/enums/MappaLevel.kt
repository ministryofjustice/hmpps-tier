package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusRegistration

enum class MappaLevel(val registerCode: String) {
    M1("M1"),
    M2("M2"),
    M3("M3"),
    ;

    companion object {
        fun from(value: String?, typeCode: String?): MappaLevel? {
            return if (typeCode == DeliusRegistration.MAPPA) {
                entries.firstOrNull { code -> code.registerCode.equals(value, true) }
            } else {
                null
            }
        }
    }
}
