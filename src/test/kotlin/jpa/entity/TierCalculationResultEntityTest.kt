package uk.gov.justice.digital.hmpps.hmppstier.jpa.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel

internal class TierCalculationResultEntityTest {

  @Test
  fun `Should Construct TierCalculationResultEntity`() {

    val tierCalculationResultEntity = TierCalculationResultEntity(
      protect = tierLetterResult,
      change = tierNumberResult
    )

    assertThat(tierCalculationResultEntity.protect).isEqualTo(tierLetterResult)
    assertThat(tierCalculationResultEntity.change).isEqualTo(tierNumberResult)
  }

  companion object {
    private val tierLetterResult = TierLevel(ProtectLevel.B, 0)
    private val tierNumberResult = TierLevel(ChangeLevel.TWO, 0)
  }
}
