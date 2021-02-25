package uk.gov.justice.digital.hmpps.hmppstier.service

import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.hmppstier.client.AssessmentApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusAssessments
import uk.gov.justice.digital.hmpps.hmppstier.client.KeyValue
import uk.gov.justice.digital.hmpps.hmppstier.client.Offender
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.client.Registration
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AssessmentComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.RsrThresholds
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@ExtendWith(MockKExtension::class)
@DisplayName("Tier Calculation Service tests")
internal class ProtectLevelCalculatorTest {

  private val clock = Clock.fixed(LocalDateTime.of(2020, 1, 1, 0, 0).toInstant(ZoneOffset.UTC), ZoneId.systemDefault())
  private val communityApiClient: CommunityApiClient = mockk(relaxUnitFun = true)
  private val assessmentApiClient: AssessmentApiClient = mockk(relaxUnitFun = true)

  private val service = ProtectLevelCalculator(
    clock,
    communityApiClient,
    assessmentApiClient
  )

  private val crn = "Any Crn"

  @BeforeEach
  fun resetAllMocks() {
    clearMocks(communityApiClient)
    clearMocks(assessmentApiClient)
  }

  @AfterEach
  fun confirmVerified() {
    // Check we don't add any more calls without updating the tests
    confirmVerified(communityApiClient)
    confirmVerified(assessmentApiClient)
  }

  @Nested
  @DisplayName("Simple Risk tests")
  inner class SimpleRiskTests {

    @Test // RsrThresholds.TIER_B_RSR
    fun `should use RSR when higher than ROSH`() {
      setup()
      // rsr B+1 = 20 points, Rosh.Medium = 10 Points
      val result = service.calculateProtectLevel(crn, null, getValidAssessments(RsrThresholds.TIER_B_RSR), getValidRegistrations(Rosh.MEDIUM), listOf())
      assertThat(result.points).isEqualTo(20)
      validate()
    }

    @Test
    fun `should use ROSH when higher than RSR`() {
      setup()
      // rsr B+1 = 20 points, Rosh.VeryHigh = 30 Points
      val result = service.calculateProtectLevel(crn, null, getValidAssessments(RsrThresholds.TIER_B_RSR), getValidRegistrations(Rosh.VERY_HIGH), listOf())
      assertThat(result.points).isEqualTo(30)
      validate()
    }

    @Test
    fun `should use either when RSR is same as ROSH`() {
      setup()
      // rsr C+1 = 10 points, Rosh.Medium = 10 Points
      val result = service.calculateProtectLevel(crn, null, getValidAssessments(RsrThresholds.TIER_C_RSR), getValidRegistrations(Rosh.MEDIUM), listOf())
      assertThat(result.points).isEqualTo(10)
      validate()
    }

    private fun getValidAssessments(rsr: RsrThresholds): DeliusAssessments {
      return DeliusAssessments(
        rsr = rsr.num.plus(BigDecimal(1)),
        ogrs = null
      )
    }

    private fun getValidRegistrations(rosh: Rosh): Collection<Registration> {
      return listOf(Registration(type = KeyValue(rosh.registerCode, "Not Used"), registerLevel = null, active = true, startDate = LocalDate.now(clock)))
    }

    private fun setup() {
      every { communityApiClient.getOffender(crn) } returns Offender("Female")
    }

    private fun validate() {
      verify { communityApiClient.getOffender(crn) }
    }
  }

  @Nested
  @DisplayName("Simple RSR tests")
  inner class SimpleRSRTests {

    @Test
    fun `should return 20 for RSR equal to tier B`() {
      setup()
      val result = service.calculateProtectLevel(crn, null, getValidAssessments(RsrThresholds.TIER_B_RSR.num), listOf(), listOf())
      assertThat(result.points).isEqualTo(20)
      validate()
    }

    @Test
    fun `should return 20 for RSR greater than tier B`() {
      setup()
      val result = service.calculateProtectLevel(crn, null, getValidAssessments(RsrThresholds.TIER_B_RSR.num.plus(BigDecimal(1))), listOf(), listOf())
      assertThat(result.points).isEqualTo(20)
      validate()
    }

    @Test
    fun `should return 10 for RSR equal to tier C`() {
      setup()
      val result = service.calculateProtectLevel(crn, null, getValidAssessments(RsrThresholds.TIER_C_RSR.num), listOf(), listOf())
      assertThat(result.points).isEqualTo(10)
      validate()
    }

    @Test
    fun `should return 10 for RSR greater than tier C`() {
      setup()
      val result = service.calculateProtectLevel(crn, null, getValidAssessments(RsrThresholds.TIER_C_RSR.num.plus(BigDecimal(1))), listOf(), listOf())
      assertThat(result.points).isEqualTo(10)
      validate()
    }

    @Test
    fun `should return 0 for RSR less than tier C`() {
      setup()
      val result = service.calculateProtectLevel(crn, null, getValidAssessments(RsrThresholds.TIER_C_RSR.num.minus(BigDecimal(1))), listOf(), listOf())
      assertThat(result.points).isEqualTo(0)
      validate()
    }

    @Test
    fun `should return 0 for RSR null`() {
      setup()
      val result = service.calculateProtectLevel(crn, null, getValidAssessments(null), listOf(), listOf())
      assertThat(result.points).isEqualTo(0)
      validate()
    }

    private fun getValidAssessments(rsr: BigDecimal?): DeliusAssessments {
      return DeliusAssessments(
        rsr = rsr,
        ogrs = null
      )
    }

    private fun setup() {
      every { communityApiClient.getOffender(crn) } returns Offender("Female")
    }

    private fun validate() {
      verify { communityApiClient.getOffender(crn) }
    }
  }

  @Nested
  @DisplayName("Simple ROSH tests")
  inner class SimpleRoshTests {

    @Test
    fun `should return 30 for Very High Rosh`() {
      setup()
      val result = service.calculateProtectLevel(crn, null, null, getValidRegistrations(Rosh.VERY_HIGH), listOf())
      assertThat(result.points).isEqualTo(30)
      validate()
    }

    @Test
    fun `should return 30 for High Rosh`() {
      setup()
      val result = service.calculateProtectLevel(crn, null, null, getValidRegistrations(Rosh.HIGH), listOf())
      assertThat(result.points).isEqualTo(20)
      validate()
    }

    @Test
    fun `should return 10 for Medium Rosh`() {
      setup()
      val result = service.calculateProtectLevel(crn, null, null, getValidRegistrations(Rosh.MEDIUM), listOf())
      assertThat(result.points).isEqualTo(10)
      validate()
    }

    @Test
    fun `should return 0 for No Rosh`() {
      setup()
      val result = service.calculateProtectLevel(crn, null, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(0)
      validate()
    }

    private fun getValidRegistrations(rosh: Rosh, active: Boolean = true): Collection<Registration> {
      return listOf(Registration(type = KeyValue(rosh.registerCode, "Not Used"), registerLevel = null, active = active, startDate = LocalDate.now(clock)))
    }

    private fun setup() {
      every { communityApiClient.getOffender(crn) } returns Offender("Female")
    }

    private fun validate() {
      verify { communityApiClient.getOffender(crn) }
    }
  }

  @Nested
  @DisplayName("Simple Mappa tests")
  inner class SimpleMappaTests {

    @Test
    fun `should return 30 for Mappa level 3`() {
      setup()
      val result = service.calculateProtectLevel(crn, null, null, getValidRegistrations(Mappa.M3), listOf())
      assertThat(result.points).isEqualTo(30)
      validate()
    }

    @Test
    fun `should return 30 for Mappa level 2`() {
      setup()
      val result = service.calculateProtectLevel(crn, null, null, getValidRegistrations(Mappa.M2), listOf())
      assertThat(result.points).isEqualTo(30)
      validate()
    }

    @Test
    fun `should return 5 for Mappa level 1`() {
      setup()
      val result = service.calculateProtectLevel(crn, null, null, getValidRegistrations(Mappa.M1), listOf())
      assertThat(result.points).isEqualTo(5)
      validate()
    }

    @Test
    fun `should return 0 for Mappa null`() {
      setup()
      val result = service.calculateProtectLevel(crn, null, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(0)
      validate()
    }

    private fun getValidRegistrations(mappa: Mappa, active: Boolean = true): Collection<Registration> {
      return listOf(Registration(type = KeyValue("Not Used", "Not Used"), registerLevel = KeyValue(mappa.registerCode, "Not Used"), active = active, startDate = LocalDate.now(clock)))
    }

    private fun setup() {
      every { communityApiClient.getOffender(crn) } returns Offender("Female")
    }

    private fun validate() {
      verify { communityApiClient.getOffender(crn) }
    }
  }

  @Nested
  @DisplayName("Simple Complexity tests")
  inner class SimpleComplexityTests {

    @Test
    fun `should count complexity factors `() {
      setup()
      val result = service.calculateProtectLevel(
        crn, null, null,
        getValidRegistrations(
          listOf(
            ComplexityFactor.VULNERABILITY_ISSUE,
            ComplexityFactor.ADULT_AT_RISK,

          )
        ),
        listOf()
      )
      assertThat(result.points).isEqualTo(4)
      validate()
    }

    @Test
    fun `should not count complexity factors duplicates`() {
      setup()
      val result = service.calculateProtectLevel(
        crn, null, null,
        getValidRegistrations(
          listOf(
            ComplexityFactor.VULNERABILITY_ISSUE,
            ComplexityFactor.VULNERABILITY_ISSUE,
          )
        ),
        listOf()
      )
      assertThat(result.points).isEqualTo(2)
      validate()
    }

    @Test
    fun `should not count complexity IOM_NOMINAL`() {
      setup()
      val result = service.calculateProtectLevel(
        crn, null, null,
        getValidRegistrations(
          listOf(
            ComplexityFactor.VULNERABILITY_ISSUE,
            ComplexityFactor.IOM_NOMINAL,
          )
        ),
        listOf()
      )
      assertThat(result.points).isEqualTo(2)
      validate()
    }

    @Test
    fun `should not count complexity factors none`() {
      setup()
      val result = service.calculateProtectLevel(crn, null, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(0)
      validate()
    }

    private fun getValidRegistrations(factors: List<ComplexityFactor>): Collection<Registration> {
      return factors.map {
        Registration(type = KeyValue(it.registerCode, "Not Used"), registerLevel = null, active = true, startDate = LocalDate.now(clock))
      }
    }

    private fun setup() {
      every { communityApiClient.getOffender(crn) } returns Offender("Female")
    }

    private fun validate() {
      verify { communityApiClient.getOffender(crn) }
    }
  }

  @Nested
  @DisplayName("Female Complexity tests")
  inner class FemaleComplexityTests {
    @Test
    fun `should not count assessment complexity factors duplicates`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "Y",
          AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "Y",
          AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "Y",
          AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "Y",
          AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "Y",
          AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "Y",
        )

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(2)

      verify { communityApiClient.getOffender(crn) }
      verify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should add multiple complexity factors`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "Y",
          AssessmentComplexityFactor.TEMPER_CONTROL to "1",
        )

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(4)

      verify { communityApiClient.getOffender(crn) }
      verify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should not include complexity factors if no valid assessment`() {
      val assessment = null

      every { communityApiClient.getOffender(crn) } returns Offender("Female")

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getOffender(crn) }
    }

    @Test
    fun `should count both Temper and Impulsivity as max '1'`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AssessmentComplexityFactor.IMPULSIVITY to "2",
          AssessmentComplexityFactor.TEMPER_CONTROL to "1",
        )

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(2) // 1 * 2 weighting for all complexity factors

      verify { communityApiClient.getOffender(crn) }
      verify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should count Temper without Impulsivity as max '2'`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AssessmentComplexityFactor.TEMPER_CONTROL to "1",
        )

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(2) // 1 * 2 weighting for all complexity factors

      verify { communityApiClient.getOffender(crn) }
      verify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should count Impulsivity without Temper as max '1'`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AssessmentComplexityFactor.IMPULSIVITY to "2",
        )

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(2) // 1 * 2 weighting for all complexity factors

      verify { communityApiClient.getOffender(crn) }
      verify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should ignore negative Parenting`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "N",
        )

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getOffender(crn) }
      verify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should ignore negative Impulsivity`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AssessmentComplexityFactor.IMPULSIVITY to "0",
        )

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getOffender(crn) }
      verify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should ignore negative Temper`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AssessmentComplexityFactor.TEMPER_CONTROL to "0",
        )

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getOffender(crn) }
      verify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should not count female only if male`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)

      every { communityApiClient.getOffender(crn) } returns Offender("Male")

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getOffender(crn) }
    }
  }
}
