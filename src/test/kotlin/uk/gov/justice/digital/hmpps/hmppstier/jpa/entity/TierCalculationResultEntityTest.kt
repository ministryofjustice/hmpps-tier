package uk.gov.justice.digital.hmpps.hmppstier.jpa.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.CalculationRule
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel

internal class TierCalculationResultEntityTest {

  @Test
  fun `Should Construct TierCalculationResultEntity`() {

    val tierCalculationResultEntity = TierCalculationResultEntity(
      protect = tierLetterResult,
      change = tierNumberResult,
      calculationVersion = 99
    )

    assertThat(tierCalculationResultEntity.protect).isEqualTo(tierLetterResult)
    assertThat(tierCalculationResultEntity.change).isEqualTo(tierNumberResult)
    assertThat(tierCalculationResultEntity.calculationVersion).isEqualTo(99)
  }

  companion object {
    private val tierLetterResult = TierLevel(ProtectLevel.B, 4, mapOf(CalculationRule.ROSH to 4))
    private val tierNumberResult = TierLevel(ChangeLevel.TWO, 12, mapOf(CalculationRule.COMPLEXITY to 12))
  }
}
