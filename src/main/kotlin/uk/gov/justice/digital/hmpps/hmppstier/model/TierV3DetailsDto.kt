package uk.gov.justice.digital.hmpps.hmppstier.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import java.time.LocalDateTime
import java.util.*

data class TierV3DetailsDto @JsonCreator constructor(

    @Schema(description = "Tier Score", example = "D")
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

    @Schema(description = "Whether the tier score is provisional", example = "false")
    @JsonProperty("provisional")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val provisional: Boolean? = null,
)
