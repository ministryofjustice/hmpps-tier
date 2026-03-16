package uk.gov.justice.digital.hmpps.hmppstier.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.*

data class TierDto @JsonCreator constructor(

    @Schema(description = "Tier Score", example = "D2")
    @JsonProperty("tierScore")
    val tierScore: String,

    @Schema(description = "Calculation Id", example = "123e4567-e89b-12d3-a456-426614174000")
    @JsonProperty("calculationId")
    val calculationId: UUID,

    @Schema(description = "Calculation Date Time", example = "2021-04-23T18:25:43.511Z")
    @JsonProperty("calculationDate")
    val calculationDate: LocalDateTime,

    @Schema(description = "Calculation Change Reason", example = "A registration was added")
    @JsonProperty("changeReason")
    val changeReason: String?,
) {
    companion object {
        private const val UNSUPERVISED_SUFFIX = "S"
        fun getSuffix(unsupervised: Boolean?) =
            if (unsupervised == true) UNSUPERVISED_SUFFIX else ""
    }
}
