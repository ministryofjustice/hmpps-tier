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
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierResult
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeScore
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectScore
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import java.time.LocalDateTime

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
      val firstTierCalculation = TierCalculationEntity(crn = crn, created = created, data = data)

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
      val firstTierCalculation = TierCalculationEntity(crn = crn, created = created, data = data)
      val secondTierCalculation = TierCalculationEntity(crn = "$crn 2", created = created.minusSeconds(1), data = data)

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
      protect = TierResult(ProtectScore.B, 0, setOf()),
      change = TierResult(ChangeScore.TWO, 0, setOf())
    )
  }
}
