package uk.gov.justice.digital.hmpps.hmppstier.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity

internal class TierDtoTest {

  @Test
  fun `Should construct TierDTO`() {

    val protectLevel = ProtectLevel.A
    val changeLevel = ChangeLevel.TWO

    val tierDto = TierDto(
      protectLevel,
      5,
      changeLevel,
      12
    )

    assertThat(tierDto.protectLevel).isEqualTo(protectLevel)
    assertThat(tierDto.changeLevel).isEqualTo(changeLevel)

    assertThat(tierDto.protectPoints).isEqualTo(5)
    assertThat(tierDto.changePoints).isEqualTo(12)
  }

  @Test
  fun `Should construct TierDTO from`() {

    val protectLevel = ProtectLevel.A
    val changeLevel = ChangeLevel.TWO

    val data = TierCalculationResultEntity(
      protect = TierLevel(protectLevel, 5),
      change = TierLevel(changeLevel, 12)
    )

    val tierDto = TierDto.from(data)

    assertThat(tierDto.protectLevel).isEqualTo(protectLevel)
    assertThat(tierDto.changeLevel).isEqualTo(changeLevel)

    assertThat(tierDto.protectPoints).isEqualTo(5)
    assertThat(tierDto.changePoints).isEqualTo(12)
  }
}
