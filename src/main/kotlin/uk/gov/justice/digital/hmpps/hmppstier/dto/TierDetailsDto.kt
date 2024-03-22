package uk.gov.justice.digital.hmpps.hmppstier.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppstier.dto.TierDto.Companion.getSuffix
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import java.time.LocalDateTime
import java.util.*

data class TierDetailsDto @JsonCreator constructor(

    @Schema(description = "Tier Score", example = "D2")
    @JsonProperty("tierScore")
    val tierScore: String,

    @Schema(description = "Calculation Id", example = "123e4567-e89b-12d3-a456-426614174000")
    @JsonProperty("calculationId")
    val calculationId: UUID,

    @Schema(description = "Calculation Date Time", example = "2021-04-23T18:25:43.511Z")
    @JsonProperty("calculationDate")
    val calculationDate: LocalDateTime,

    @Schema(description = "Calculation input data")
    @JsonProperty("data")
    val data: TierCalculationResultEntity,
) {

    companion object {
        fun from(calculation: TierCalculationEntity, includeSuffix: Boolean) = TierDetailsDto(
            calculation.data.protect.tier.value.plus(calculation.data.change.tier.value)
                .plus(getSuffix(calculation.data.deliusInputs?.registrations?.unsupervised, includeSuffix)),
            calculation.uuid,
            calculation.created,
            calculation.data,
        )
    }
}
