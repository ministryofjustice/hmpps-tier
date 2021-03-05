package uk.gov.justice.digital.hmpps.hmppstier.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import java.util.UUID

@ApiModel(description = "Tier")
data class TierDto @JsonCreator constructor(

  @ApiModelProperty(value = "Protect Level", example = "D")
  @JsonProperty("protectLevel")
  val protectLevel: ProtectLevel,

  @ApiModelProperty(value = "Protect Points", example = "17")
  @JsonProperty("protectPoints")
  val protectPoints: Int?,

  @ApiModelProperty(value = "Change Level", example = "2")
  @JsonProperty("changeLevel")
  val changeLevel: ChangeLevel,

  @ApiModelProperty(value = "Change Points", example = "12")
  @JsonProperty("changePoints")
  val changePoints: Int?,

  @ApiModelProperty(value = "Calculation Id", example = "123e4567-e89b-12d3-a456-426614174000")
  @JsonProperty("calculationId")
  val calculationId: UUID?,

) {

  companion object {
    fun from(calculation: TierCalculationEntity): TierDto {
      return TierDto(
        calculation.data.protect.tier,
        calculation.data.protect.points,
        calculation.data.change.tier,
        calculation.data.change.points,
        calculation.uuid
      )
    }
  }
}
