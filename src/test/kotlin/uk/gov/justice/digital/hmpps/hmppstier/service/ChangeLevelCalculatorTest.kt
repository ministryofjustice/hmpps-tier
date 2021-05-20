package uk.gov.justice.digital.hmpps.hmppstier.service

import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.domain.Sentence
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@ExtendWith(MockKExtension::class)
@DisplayName("Change Level Calculator tests")
internal class ChangeLevelCalculatorTest {
  private val clock = Clock.fixed(LocalDateTime.of(2020, 1, 1, 0, 0).toInstant(ZoneOffset.UTC), ZoneId.systemDefault())
  private val communityApiClient: CommunityApiClient = mockk(relaxUnitFun = true)

  private val service = ChangeLevelCalculator(
    MandateForChange(communityApiClient),
  )

  private val crn = "Any Crn"

  @BeforeEach
  fun resetAllMocks() {
    clearMocks(communityApiClient)
  }

  @AfterEach
  fun confirmVerified() {
    // Check we don't add any more calls without updating the tests
    confirmVerified(communityApiClient)
  }

  @Nested
  @DisplayName("Simple Oasys Needs tests")
  inner class SimpleNeedsTests {

    @Test
    fun `should calculate Oasys Needs none`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      val result = service.calculateChangeLevel(crn, assessment, 0, listOf(), getValidConviction(), mapOf())
      assertThat(result.points).isEqualTo(0)
    }

    private fun getValidConviction(): List<Conviction> {
      return listOf(Conviction(54321L, Sentence(null, "SC", LocalDate.now(clock), LocalDate.now(clock).plusDays(1)), listOf(), "101"))
    }
  }

  @Nested
  @DisplayName("Simple Ogrs tests")
  inner class SimpleOgrsTests {

    @Test
    fun `should calculate Ogrs null`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")
      val result = service.calculateChangeLevel(crn, assessment, 0, listOf(), getValidConviction(), mapOf())
      assertThat(result.points).isEqualTo(0)
    }

    @Test
    fun `should calculate Ogrs null - no deliusAssessment`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")
      val result = service.calculateChangeLevel(crn, assessment, 0, listOf(), getValidConviction(), mapOf())
      assertThat(result.points).isEqualTo(0)
    }

    private fun getValidConviction(): List<Conviction> {
      return listOf(Conviction(54321L, Sentence(null, "SC", LocalDate.now(clock), LocalDate.now(clock).plusDays(1)), listOf(), "101"))
    }
  }
}
