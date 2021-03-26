package uk.gov.justice.digital.hmpps.hmppstier.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import java.util.UUID

@ApiModel(description = "Tier")
data class TierDto @JsonCreator constructor(

  @ApiModelProperty(value = "Tier Score", example = "D2")
  @JsonProperty("tierScore")
  val tierScore: String,

  @ApiModelProperty(value = "Calculation Id", example = "123e4567-e89b-12d3-a456-426614174000")
  @JsonProperty("calculationId")
  val calculationId: UUID,

) {

  companion object {
    fun from(calculation: TierCalculationEntity): TierDto {
      return TierDto(
        calculation.data.protect.tier.value.plus(calculation.data.change.tier.value),
        calculation.uuid
      )
    }
  }
}
