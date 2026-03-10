package uk.gov.justice.digital.hmpps.hmppstier.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppstier.jpa.v1.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.v1.entity.TierSummaryEntity
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
        fun from(calculation: TierCalculationEntity) = TierDto(
            tierScore = calculation.protectLevel() + calculation.changeLevel() + getSuffix(calculation.data.deliusInputs?.registrations?.unsupervised),
            calculationId = calculation.uuid,
            calculationDate = calculation.created,
            changeReason = calculation.changeReason
        )

        fun from(summary: TierSummaryEntity) = TierDto(
            tierScore = summary.protectLevel + summary.changeLevel + getSuffix(summary.unsupervised),
            calculationId = summary.uuid,
            calculationDate = summary.lastModified,
            changeReason = null
        )

        fun getSuffix(unsupervised: Boolean?) =
            if (unsupervised == true) UNSUPERVISED_SUFFIX else ""
    }
}
