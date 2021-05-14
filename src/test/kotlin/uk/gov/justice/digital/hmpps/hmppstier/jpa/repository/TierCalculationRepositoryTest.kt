package uk.gov.justice.digital.hmpps.hmppstier.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.CalculationRule
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import java.time.LocalDateTime
import java.util.UUID

@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class TierCalculationRepositoryTest(
  @Autowired val repository: TierCalculationRepository
) {

  @BeforeEach
  fun resetDatabase() {
    repository.deleteAll()
  }

  @Nested
  @DisplayName("Get Latest Calculation Tests")
  inner class GetLatestCalculationTests {

    @Test
    fun `Should return latest calculation when only one`() {

      val created = LocalDateTime.now()
      val firstTierCalculation = TierCalculationEntity(crn = crn, created = created, data = data, uuid = UUID.randomUUID())

      repository.save(firstTierCalculation)

      val calculation = repository.findFirstByCrnOrderByCreatedDesc(crn)
      assertThat(calculation).isNotNull
      assertThat(calculation?.crn).isEqualTo(firstTierCalculation.crn)
      assertThat(calculation?.created).isEqualToIgnoringNanos(firstTierCalculation.created)
      assertThat(calculation?.data).isEqualTo(firstTierCalculation.data)
    }

    @Test
    fun `Should return latest calculation none`() {
      val calculation = repository.findFirstByCrnOrderByCreatedDesc(crn)
      assertThat(calculation).isNull()
    }

    @Test
    fun `Should return latest calculation when multiple`() {

      val created = LocalDateTime.now()
      val firstTierCalculation = TierCalculationEntity(crn = crn, created = created, data = data, uuid = UUID.randomUUID())
      val secondTierCalculation = TierCalculationEntity(crn = crn, created = created.minusSeconds(1), data = data, uuid = UUID.randomUUID())

      repository.save(firstTierCalculation)
      repository.save(secondTierCalculation)

      val calculation = repository.findFirstByCrnOrderByCreatedDesc(crn)
      assertThat(calculation).isNotNull
      assertThat(calculation?.crn).isEqualTo(firstTierCalculation.crn)
      assertThat(calculation?.created).isEqualToIgnoringNanos(firstTierCalculation.created)
      assertThat(calculation?.data).isEqualTo(firstTierCalculation.data)
    }
  }

  @Nested
  @DisplayName("Get Calculation By Id Tests")
  inner class GetCalculationByIdTests {

    @Test
    fun `Should return calculation by ID when only one`() {

      val calculationId = UUID.randomUUID()
      val created = LocalDateTime.now()
      val firstTierCalculation = TierCalculationEntity(crn = crn, created = created, data = data, uuid = calculationId)

      repository.save(firstTierCalculation)

      val calculation = repository.findByCrnAndUuid(crn, calculationId)
      assertThat(calculation).isNotNull
      assertThat(calculation?.crn).isEqualTo(firstTierCalculation.crn)
      assertThat(calculation?.created).isEqualToIgnoringNanos(firstTierCalculation.created)
      assertThat(calculation?.data).isEqualTo(firstTierCalculation.data)
    }

    @Test
    fun `Should return latest calculation none`() {

      val calculationId = UUID.randomUUID()
      val calculation = repository.findByCrnAndUuid(crn, calculationId)
      assertThat(calculation).isNull()
    }

    @Test
    fun `Should return calculation by Id when multiple`() {

      val calculationId = UUID.randomUUID()
      val created = LocalDateTime.now()
      val firstTierCalculation = TierCalculationEntity(crn = crn, created = created, data = data, uuid = calculationId)
      val secondTierCalculation = TierCalculationEntity(crn = crn, created = created.minusSeconds(1), data = data, uuid = UUID.randomUUID())

      repository.save(firstTierCalculation)
      repository.save(secondTierCalculation)

      val calculation = repository.findFirstByCrnOrderByCreatedDesc(crn)
      assertThat(calculation).isNotNull
      assertThat(calculation?.crn).isEqualTo(firstTierCalculation.crn)
      assertThat(calculation?.created).isEqualToIgnoringNanos(firstTierCalculation.created)
      assertThat(calculation?.data).isEqualTo(firstTierCalculation.data)
    }
  }

  companion object {
    private const val crn = "Any CRN"
    private val data = TierCalculationResultEntity(
      protect = TierLevel(ProtectLevel.B, 4, mapOf(CalculationRule.ROSH to 4)),
      change = TierLevel(ChangeLevel.TWO, 12, mapOf(CalculationRule.COMPLEXITY to 12)),
      calculationVersion = 99
    )
  }
}
