package uk.gov.justice.digital.hmpps.hmppstier.client.delius

import com.fasterxml.jackson.annotation.JsonCreator
import java.time.LocalDate

data class DeliusRegistration @JsonCreator constructor(
    val code: String,
    val level: String?,
    val date: LocalDate,
) {
    companion object {
        const val TWO_THIRDS_CODE = "PRC"
    }
}