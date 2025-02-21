package uk.gov.justice.digital.hmpps.hmppstier.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierSummary
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
        fun from(calculation: TierCalculationEntity, includeSuffix: Boolean): TierDto {
            return TierDto(
                "${calculation.protectLevel()}${calculation.changeLevel()}${
                    getSuffix(
                        calculation.data.deliusInputs?.registrations?.unsupervised,
                        includeSuffix
                    )
                }",
                calculation.uuid,
                calculation.created,
                calculation.changeReason
            )
        }

        fun from(summary: TierSummary, includeSuffix: Boolean): TierDto {
            return TierDto(
                "${summary.protectLevel}${summary.changeLevel}${getSuffix(summary.unsupervised, includeSuffix)}",
                summary.uuid,
                summary.lastModified,
                null
            )
        }

        fun getSuffix(unsupervised: Boolean?, includeSuffix: Boolean) =
            if (unsupervised == true && includeSuffix) UNSUPERVISED_SUFFIX else ""
    }
}
