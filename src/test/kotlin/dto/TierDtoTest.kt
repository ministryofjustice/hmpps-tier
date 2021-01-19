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

    val protectTier = ProtectLevel.A
    val changeTier = ChangeLevel.TWO

    val tierDto = TierDto(
      protectTier,
      5,
      changeTier,
      12
    )

    assertThat(tierDto.protectTier).isEqualTo(protectTier)
    assertThat(tierDto.changeTier).isEqualTo(changeTier)

    assertThat(tierDto.protectScore).isEqualTo(5)
    assertThat(tierDto.changeScore).isEqualTo(12)
  }

  @Test
  fun `Should construct TierDTO from`() {

    val protectTier = ProtectLevel.A
    val changeTier = ChangeLevel.TWO

    val data = TierCalculationResultEntity(
      protect = TierLevel(protectTier, 5),
      change = TierLevel(changeTier, 12)
    )

    val tierDto = TierDto.from(data)

    assertThat(tierDto.protectTier).isEqualTo(protectTier)
    assertThat(tierDto.changeTier).isEqualTo(changeTier)

    assertThat(tierDto.protectScore).isEqualTo(5)
    assertThat(tierDto.changeScore).isEqualTo(12)
  }
}
