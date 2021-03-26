package uk.gov.justice.digital.hmpps.hmppstier.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import java.time.LocalDateTime
import java.util.UUID

internal class TierDtoTest {

  @Test
  fun `Should construct TierDTO`() {

    val protectLevel = ProtectLevel.A
    val changeLevel = ChangeLevel.TWO
    val calculationId = UUID.randomUUID()
    val calculationDate = LocalDateTime.now()

    val tierDto = TierDto(
      protectLevel.value.plus(changeLevel.value),
      calculationId,
      calculationDate
    )

    assertThat(tierDto.tierScore).isEqualTo(protectLevel.value.plus(changeLevel.value))
    assertThat(tierDto.calculationId).isEqualTo(calculationId)
    assertThat(tierDto.calculationDate).isEqualTo(calculationDate)
  }

  @Test
  fun `Should construct TierDTO from`() {

    val protectLevel = ProtectLevel.A
    val changeLevel = ChangeLevel.TWO
    val calculationId = UUID.randomUUID()
    val calculationDate = LocalDateTime.now()

    val data = TierCalculationResultEntity(
      protect = TierLevel(protectLevel, 5),
      change = TierLevel(changeLevel, 12)
    )

    val entity = TierCalculationEntity(
      0,
      calculationId,
      "Any Crn",
      calculationDate,
      data
    )

    val tierDto = TierDto.from(entity)

    assertThat(tierDto.tierScore).isEqualTo(protectLevel.value.plus(changeLevel.value))
    assertThat(tierDto.calculationId).isEqualTo(calculationId)
    assertThat(tierDto.calculationDate).isEqualTo(calculationDate)
  }
}
