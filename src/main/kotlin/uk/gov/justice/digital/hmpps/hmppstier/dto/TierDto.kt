package uk.gov.justice.digital.hmpps.hmppstier.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity

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

) {
  companion object {
    fun from(calculation: TierCalculationResultEntity): TierDto {
      return TierDto(
        calculation.protect.tier,
        calculation.protect.points,
        calculation.change.tier,
        calculation.change.points
      )
    }
  }
}
