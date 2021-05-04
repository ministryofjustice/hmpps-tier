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
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusAssessments
import uk.gov.justice.digital.hmpps.hmppstier.client.KeyValue
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.client.Sentence
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
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
  private val assessmentApiService: AssessmentApiService = mockk(relaxUnitFun = true)

  private val service = ChangeLevelCalculator(
    MandateForChange(communityApiClient),
    assessmentApiService
  )

  private val crn = "Any Crn"

  @BeforeEach
  fun resetAllMocks() {
    clearMocks(communityApiClient)
    clearMocks(assessmentApiService)
  }

  @AfterEach
  fun confirmVerified() {
    // Check we don't add any more calls without updating the tests
    confirmVerified(communityApiClient)
    confirmVerified(assessmentApiService)
  }

  @Nested
  @DisplayName("Simple Oasys Needs tests")
  inner class SimpleNeedsTests {

    @Test
    fun `should calculate Oasys Needs none`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) } returns mapOf()

      val result = service.calculateChangeLevel(crn, assessment, null, listOf(), getValidConviction())
      assertThat(result.points).isEqualTo(0)

      verify { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) }
    }

    @Test
    fun `should add Oasys Needs no need`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) } returns mapOf(
        Need.ACCOMMODATION to NeedSeverity.NO_NEED, // 0
      )

      val result = service.calculateChangeLevel(crn, assessment, null, listOf(), getValidConviction())
      assertThat(result.points).isEqualTo(0)

      verify { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) }
    }

    @Test
    fun `should add Oasys Needs standard need`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) } returns mapOf(
        Need.ACCOMMODATION to NeedSeverity.STANDARD, // 1
      )

      val result = service.calculateChangeLevel(crn, assessment, null, listOf(), getValidConviction())
      assertThat(result.points).isEqualTo(1)

      verify { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) }
    }

    @Test
    fun `should add Oasys Needs severe need`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) } returns mapOf(
        Need.ACCOMMODATION to NeedSeverity.SEVERE, // 2
      )

      val result = service.calculateChangeLevel(crn, assessment, null, listOf(), getValidConviction())
      assertThat(result.points).isEqualTo(2)

      verify { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) }
    }

    @Test
    fun `should add multiple Oasys Needs`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) } returns mapOf(
        Need.ACCOMMODATION to NeedSeverity.SEVERE, // 2
        Need.EDUCATION_TRAINING_AND_EMPLOYABILITY to NeedSeverity.SEVERE, // 2
        Need.RELATIONSHIPS to NeedSeverity.SEVERE, // 2
        Need.LIFESTYLE_AND_ASSOCIATES to NeedSeverity.SEVERE, // 2
        Need.DRUG_MISUSE to NeedSeverity.SEVERE, // 2
      )

      val result = service.calculateChangeLevel(crn, assessment, null, listOf(), getValidConviction())
      assertThat(result.points).isEqualTo(10)

      verify { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) }
    }

    private fun getValidConviction(): List<Conviction> {
      return listOf(Conviction(54321L, Sentence(null, KeyValue("SC"), LocalDate.now(clock), LocalDate.now(clock).plusDays(1)), listOf(), "101"))
    }
  }

  @Nested
  @DisplayName("Simple Ogrs tests")
  inner class SimpleOgrsTests {

    @Test
    fun `should calculate Ogrs null`() {
      setup()
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")
      val result = service.calculateChangeLevel(crn, assessment, getValidAssessments(null), listOf(), getValidConviction())
      assertThat(result.points).isEqualTo(0)
      validate()
    }

    @Test
    fun `should calculate Ogrs null - no deliusAssessment`() {
      setup()
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")
      val result = service.calculateChangeLevel(crn, assessment, null, listOf(), getValidConviction())
      assertThat(result.points).isEqualTo(0)
      validate()
    }

    private fun getValidAssessments(ogrs: Int?): DeliusAssessments {
      return DeliusAssessments(
        rsr = null,
        ogrs = ogrs
      )
    }

    private fun getValidConviction(): List<Conviction> {
      return listOf(Conviction(54321L, Sentence(null, KeyValue("SC"), LocalDate.now(clock), LocalDate.now(clock).plusDays(1)), listOf(), "101"))
    }

    private fun setup() {
      every { assessmentApiService.getAssessmentNeeds(any()) } returns mapOf()
    }

    private fun validate() {
      verify { assessmentApiService.getAssessmentNeeds(any()) }
    }
  }
}
