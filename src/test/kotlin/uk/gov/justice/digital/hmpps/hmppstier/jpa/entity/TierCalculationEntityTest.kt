package uk.gov.justice.digital.hmpps.hmppstier.jpa.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import java.time.LocalDateTime
import java.util.UUID

internal class TierCalculationEntityTest {

  @Test
  fun `Should Construct TierCalculationEntity`() {
    val crn = "Any CRN"
    val created = LocalDateTime.now()
    val data = data
    val calculationId = UUID.randomUUID()

    val tierCalculationResultEntity = TierCalculationEntity(crn = crn, created = created, data = data, uuid = calculationId)

    assertThat(tierCalculationResultEntity.crn).isEqualTo(crn)
    assertThat(tierCalculationResultEntity.created).isEqualTo(created)
    assertThat(tierCalculationResultEntity.data).isEqualTo(data)

    assertThat(tierCalculationResultEntity.uuid).isEqualTo(calculationId)
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
