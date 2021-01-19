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

  @ApiModelProperty(value = "Protect Tier", example = "D")
  @JsonProperty("protectTier")
  val protectTier: ProtectLevel,

  @ApiModelProperty(value = "Protect Score", example = "17")
  @JsonProperty("protectScore")
  val protectScore: Int?,

  @ApiModelProperty(value = "Change Tier", example = "2")
  @JsonProperty("changeTier")
  val changeTier: ChangeLevel,

  @ApiModelProperty(value = "Change Score", example = "12")
  @JsonProperty("changeScore")
  val changeScore: Int?,

  ) {
  companion object {
    fun from(calculation: TierCalculationResultEntity): TierDto {
      return TierDto(
        calculation.protect.tier,
        calculation.protect.score,
        calculation.change.tier,
        calculation.change.score
      )
    }
  }
}
