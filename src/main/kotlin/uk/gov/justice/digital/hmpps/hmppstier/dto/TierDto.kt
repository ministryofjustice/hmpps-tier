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

  ) {

  companion object {
    fun from(calculation: TierCalculationEntity): TierDto {
      return TierDto(
        "${calculation.protectLevel()}${calculation.changeLevel()}",
        calculation.uuid,
        calculation.created,
      )
    }

    fun from(summary: TierSummary): TierDto {
      return TierDto(
        "${summary.protectLevel}${summary.changeLevel}",
        summary.uuid,
        summary.lastModified,
      )
    }
  }
}
