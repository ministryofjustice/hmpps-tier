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

    val tierDto = TierDto(
      protectLevel,
      5,
      changeLevel,
      12,
      calculationId
    )

    assertThat(tierDto.protectLevel).isEqualTo(protectLevel)
    assertThat(tierDto.changeLevel).isEqualTo(changeLevel)

    assertThat(tierDto.protectPoints).isEqualTo(5)
    assertThat(tierDto.changePoints).isEqualTo(12)

    assertThat(tierDto.calculationId).isEqualTo(calculationId)
  }

  @Test
  fun `Should construct TierDTO from`() {

    val protectLevel = ProtectLevel.A
    val changeLevel = ChangeLevel.TWO

    val calculationId = UUID.randomUUID()

    val data = TierCalculationResultEntity(
      protect = TierLevel(protectLevel, 5),
      change = TierLevel(changeLevel, 12)
    )

    val entity = TierCalculationEntity(
      0,
      calculationId,
      "Any Crn",
      LocalDateTime.now(),
      data
    )

    val tierDto = TierDto.from(entity)

    assertThat(tierDto.protectLevel).isEqualTo(protectLevel)
    assertThat(tierDto.changeLevel).isEqualTo(changeLevel)

    assertThat(tierDto.protectPoints).isEqualTo(5)
    assertThat(tierDto.changePoints).isEqualTo(12)

    assertThat(tierDto.calculationId).isEqualTo(calculationId)
  }
}
