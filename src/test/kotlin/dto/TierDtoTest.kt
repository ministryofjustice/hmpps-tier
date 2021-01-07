package uk.gov.justice.digital.hmpps.hmppstier.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierResult
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeScore
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectScore
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity


internal class TierDtoTest {

  @Test
  fun `Should construct TierDTO`() {

    val protectTier = ProtectScore.A
    val changeTier = ChangeScore.TWO

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

    val protectTier = ProtectScore.A
    val changeTier = ChangeScore.TWO

    val data = TierCalculationResultEntity(
      protect = TierResult(protectTier, 5, setOf()),
      change = TierResult(changeTier, 12, setOf())
    )

    val tierDto = TierDto.from(data)

    assertThat(tierDto.protectTier).isEqualTo(protectTier)
    assertThat(tierDto.changeTier).isEqualTo(changeTier)

    assertThat(tierDto.protectScore).isEqualTo(5)
    assertThat(tierDto.changeScore).isEqualTo(12)
  }

}