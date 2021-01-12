package uk.gov.justice.digital.hmpps.hmppstier.jpa.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierResult
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeScore
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectScore
import java.time.LocalDateTime

internal class TierCalculationEntityTest {

  @Test
  fun `Should Construct TierCalculationEntity`() {
    val crn = "Any CRN"
    val created = LocalDateTime.now()
    val data = data

    val tierCalculationResultEntity = TierCalculationEntity(crn = crn, created = created, data = data)

    assertThat(tierCalculationResultEntity.crn).isEqualTo(crn)
    assertThat(tierCalculationResultEntity.created).isEqualTo(created)
    assertThat(tierCalculationResultEntity.data).isEqualTo(data)
  }

  companion object {
    private val tierLetterResult = TierResult(ProtectScore.B, 0, setOf())
    private val tierNumberResult = TierResult(ChangeScore.TWO, 0, setOf())

    val data = TierCalculationResultEntity(
      protect = tierLetterResult,
      change = tierNumberResult
    )
  }
}
