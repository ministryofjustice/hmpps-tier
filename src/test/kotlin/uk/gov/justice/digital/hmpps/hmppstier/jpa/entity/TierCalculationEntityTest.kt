package uk.gov.justice.digital.hmpps.hmppstier.jpa.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
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
    private val tierLetterResult = TierLevel(ProtectLevel.B, 0)
    private val tierNumberResult = TierLevel(ChangeLevel.TWO, 0)

    val data = TierCalculationResultEntity(
      protect = tierLetterResult,
      change = tierNumberResult
    )
  }
}
