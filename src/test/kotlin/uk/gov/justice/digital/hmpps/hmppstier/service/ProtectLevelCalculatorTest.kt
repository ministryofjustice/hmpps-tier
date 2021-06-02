package uk.gov.justice.digital.hmpps.hmppstier.service

import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.hmppstier.domain.Registrations
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.RsrThresholds.TIER_C_RSR_LOWER
import java.math.BigDecimal

@ExtendWith(MockKExtension::class)
@DisplayName("Protect Level Calculator tests")
internal class ProtectLevelCalculatorTest {

  private val service = ProtectLevelCalculator()

  private val crn = "Any Crn"

  private fun calculateProtectLevel(rsr: BigDecimal = BigDecimal.ZERO, rosh: Rosh? = null): TierLevel<ProtectLevel> {
    return service.calculateProtectLevel(
      rsr,
      0,
      Registrations(listOf(), listOf(), rosh, null)
    )
  }

  @Nested
  @DisplayName("Simple Risk tests")
  inner class SimpleRiskTests {

    @Test
    fun `should use either when RSR is same as ROSH`() {
      // rsr C+1 = 10 points, Rosh.Medium = 10 Points
      val result = calculateProtectLevel(rsr = TIER_C_RSR_LOWER.num, rosh = Rosh.MEDIUM)
      assertThat(result.points).isEqualTo(10)
    }
  }

  @Nested
  @DisplayName("Simple RSR tests")
  inner class SimpleRSRTests {

    @Test
    fun `should return 0 for RSR null`() {
      val result = calculateProtectLevel(rsr = BigDecimal.ZERO)
      assertThat(result.points).isEqualTo(0)
    }

    @Test
    fun `Should return RSR`() {
      val result = calculateProtectLevel(rsr = BigDecimal(5))
      assertThat(result.points).isEqualTo(10)
    }
  }
}
