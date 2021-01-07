package uk.gov.justice.digital.hmpps.hmppstier.jpa.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierCalculationResult
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierResult
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeScore
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectScore


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

  @Test
  fun `Should From TierCalculationResultEntity`() {

    val tierCalculationResult = TierCalculationResult(
      protectScore = tierLetterResult,
      changeScore = tierNumberResult
    )


    val tierCalculationResultEntity = TierCalculationResultEntity.from(tierCalculationResult)

    assertThat(tierCalculationResultEntity.protect).isEqualTo(tierLetterResult)
    assertThat(tierCalculationResultEntity.change).isEqualTo(tierNumberResult)

  }

  companion object {
    private val tierLetterResult = TierResult(ProtectScore.B, 0, setOf())
    private val tierNumberResult = TierResult(ChangeScore.TWO, 0, setOf())
  }
}