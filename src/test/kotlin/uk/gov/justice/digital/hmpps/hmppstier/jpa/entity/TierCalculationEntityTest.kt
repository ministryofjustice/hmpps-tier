package uk.gov.justice.digital.hmpps.hmppstier.jpa.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.CalculationRule
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

  @Test
  fun `can use equals and hashcode for TierCalculationEntity`() {
    val crn = "Any CRN"
    val id = 1L
    val created = LocalDateTime.now()
    val data = data
    val calculationId = UUID.randomUUID()

    val tierCalculationResultEntity = TierCalculationEntity(id, calculationId, crn, created, data)
    val tierCalculationResultEntity2 = TierCalculationEntity(id, calculationId, crn, created, data)

    assertThat(tierCalculationResultEntity).isEqualTo(tierCalculationResultEntity2)
    assertThat(tierCalculationResultEntity.toString()).isEqualTo(tierCalculationResultEntity2.toString())
    assertThat(tierCalculationResultEntity.hashCode()).isEqualTo(tierCalculationResultEntity2.hashCode())
  }

  companion object {
    private val tierLetterResult = TierLevel(ProtectLevel.A, 4, mapOf(CalculationRule.ROSH to 4))
    private val tierNumberResult = TierLevel(ChangeLevel.TWO, 12, mapOf(CalculationRule.COMPLEXITY to 12))

    val data = TierCalculationResultEntity(
      protect = tierLetterResult,
      change = tierNumberResult,
      calculationVersion = "88",
    )
  }
}
