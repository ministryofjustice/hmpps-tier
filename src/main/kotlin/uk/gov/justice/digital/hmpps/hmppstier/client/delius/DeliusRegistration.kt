package uk.gov.justice.digital.hmpps.hmppstier.client.delius

import com.fasterxml.jackson.annotation.JsonCreator
import java.time.LocalDate

data class DeliusRegistration @JsonCreator constructor(
    val code: String,
    val level: String?,
    val category: String?,
    val date: LocalDate,
) {
    companion object {
        const val MAPPA = "MAPP"
        const val LIFER = "INLL"
        const val DOMESTIC_ABUSE = "ADVP"
        const val DOMESTIC_ABUSE_HISTORY = "REG30"
        const val STALKING = "SPO"
        const val CHILD_PROTECTION = "RCPR"
        const val TWO_THIRDS_CODE = "PRC"
    }
}