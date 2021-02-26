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
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.client.Requirement
import uk.gov.justice.digital.hmpps.hmppstier.client.Sentence
import uk.gov.justice.digital.hmpps.hmppstier.client.SentenceType
import uk.gov.justice.digital.hmpps.hmppstier.client.UnpaidWork
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@ExtendWith(MockKExtension::class)
@DisplayName("Tier Calculation Service tests")
internal class ChangeLevelCalculatorTest {
  private val clock = Clock.fixed(LocalDateTime.of(2020, 1, 1, 0, 0).toInstant(ZoneOffset.UTC), ZoneId.systemDefault())
  private val communityApiClient: CommunityApiClient = mockk(relaxUnitFun = true)
  private val assessmentApiService: AssessmentApiService = mockk(relaxUnitFun = true)

  private val service = ChangeLevelCalculator(
    communityApiClient,
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
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)

      every { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) } returns mapOf()

      val result = service.calculateChangeLevel(crn, assessment, null, listOf(), getValidConviction())
      assertThat(result.points).isEqualTo(0)

      verify { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) }
    }

    @Test
    fun `should add Oasys Needs no need`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)

      every { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) } returns mapOf(
        Need.ACCOMMODATION to NeedSeverity.NO_NEED, // 0
      )

      val result = service.calculateChangeLevel(crn, assessment, null, listOf(), getValidConviction())
      assertThat(result.points).isEqualTo(0)

      verify { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) }
    }

    @Test
    fun `should add Oasys Needs standard need`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)

      every { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) } returns mapOf(
        Need.ACCOMMODATION to NeedSeverity.STANDARD, // 1
      )

      val result = service.calculateChangeLevel(crn, assessment, null, listOf(), getValidConviction())
      assertThat(result.points).isEqualTo(1)

      verify { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) }
    }

    @Test
    fun `should add Oasys Needs severe need`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)

      every { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) } returns mapOf(
        Need.ACCOMMODATION to NeedSeverity.SEVERE, // 2
      )

      val result = service.calculateChangeLevel(crn, assessment, null, listOf(), getValidConviction())
      assertThat(result.points).isEqualTo(2)

      verify { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) }
    }

    @Test
    fun `should add multiple Oasys Needs`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)

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
      return listOf(Conviction(54321L, Sentence(null, SentenceType("SC"), null)))
    }
  }

  @Nested
  @DisplayName("Simple Ogrs tests")
  inner class SimpleOgrsTests {

    @Test
    fun `should calculate Ogrs null`() {
      setup()
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)
      val result = service.calculateChangeLevel(crn, assessment, getValidAssessments(null), listOf(), getValidConviction())
      assertThat(result.points).isEqualTo(0)
      validate()
    }

    @Test
    fun `should calculate Ogrs null - no deliusAssessment`() {
      setup()
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)
      val result = service.calculateChangeLevel(crn, assessment, null, listOf(), getValidConviction())
      assertThat(result.points).isEqualTo(0)
      validate()
    }

    @Test
    fun `should calculate Ogrs take 10s 50`() {
      setup()
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)
      val result = service.calculateChangeLevel(crn, assessment, getValidAssessments(50), listOf(), getValidConviction())
      assertThat(result.points).isEqualTo(5)
      validate()
    }

    @Test
    fun `should calculate Ogrs take 10s 51`() {
      setup()
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)
      val result = service.calculateChangeLevel(crn, assessment, getValidAssessments(51), listOf(), getValidConviction())
      assertThat(result.points).isEqualTo(5)
      validate()
    }

    @Test
    fun `should calculate Ogrs take 10s 59`() {
      setup()
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)
      val result = service.calculateChangeLevel(crn, assessment, getValidAssessments(59), listOf(), getValidConviction())
      assertThat(result.points).isEqualTo(5)
      validate()
    }

    private fun getValidAssessments(ogrs: Int?): DeliusAssessments {
      return DeliusAssessments(
        rsr = null,
        ogrs = ogrs
      )
    }

    private fun getValidConviction(): List<Conviction> {
      return listOf(Conviction(54321L, Sentence(null, SentenceType("SC"), null)))
    }

    private fun setup() {
      every { assessmentApiService.getAssessmentNeeds(any()) } returns mapOf()
    }

    private fun validate() {
      verify { assessmentApiService.getAssessmentNeeds(any()) }
    }
  }

  @Nested
  @DisplayName("Simple Recent Assessment tests")
  inner class SimpleRecentAssessmentTests {

    @Test
    fun `should not calculate change tier if not recent`() {

      val result = service.calculateChangeLevel(crn, null, null, listOf(), getValidConviction())

      assertThat(result.tier).isEqualTo(ChangeLevel.TWO)
      assertThat(result.points).isEqualTo(0)
    }

    private fun getValidConviction(): List<Conviction> {
      return listOf(Conviction(54321L, Sentence(null, SentenceType("SC"), null)))
    }
  }

  @Nested
  @DisplayName("Simple Mandate for change tests")
  inner class SimpleMandateForChangeTests {

    @Test
    fun `should not calculate change tier if current noncustodial, no restrictive requirements and some unpaid work`() {
      val crn = "123"
      val conviction = Conviction(54321L, Sentence(null, SentenceType("Not a custodial type"), UnpaidWork("Some UPW")))
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)

      every { communityApiClient.getRequirements(crn, conviction.convictionId) } returns listOf()

      val result = service.calculateChangeLevel(crn, assessment, null, listOf(), listOf(conviction))

      assertThat(result.tier).isEqualTo(ChangeLevel.ZERO)
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getRequirements(crn, conviction.convictionId) }
    }

    @Test
    fun `should not calculate change tier if current noncustodial, with restrictive requirements`() {
      val crn = "123"
      val conviction = Conviction(54321L, Sentence(null, SentenceType("Not a custodial type"), null))
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)

      every { communityApiClient.getRequirements(crn, conviction.convictionId) } returns listOf(Requirement(true))

      val result = service.calculateChangeLevel(crn, assessment, null, listOf(), listOf(conviction))

      assertThat(result.tier).isEqualTo(ChangeLevel.ZERO)
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getRequirements(crn, conviction.convictionId) }
    }

    @Test
    fun `should calculate change tier if current noncustodial, with no restrictive requirements or unpaid work`() {
      val crn = "123"
      val conviction = Conviction(54321L, Sentence(null, SentenceType("Not a custodial type"), null))
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)

      every { communityApiClient.getRequirements(crn, conviction.convictionId) } returns listOf()
      every { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) } returns mapOf()

      val result = service.calculateChangeLevel(crn, assessment, null, listOf(), listOf(conviction))

      assertThat(result.tier).isEqualTo(ChangeLevel.ONE)
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getRequirements(crn, conviction.convictionId) }
      verify { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) }
    }

    @Test
    fun `should calculate change tier if current custodial SC`() {
      val crn = "123"
      val conviction = Conviction(54321L, Sentence(null, SentenceType("SC"), null))
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)

      every { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) } returns mapOf()

      val result = service.calculateChangeLevel(crn, assessment, null, listOf(), listOf(conviction))

      assertThat(result.tier).isEqualTo(ChangeLevel.ONE)
      assertThat(result.points).isEqualTo(0)

      verify { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) }
    }

    @Test
    fun `should calculate change tier if current custodial NC`() {
      val crn = "123"
      val conviction = Conviction(54321L, Sentence(null, SentenceType("NC"), null))
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null)

      every { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) } returns mapOf()

      val result = service.calculateChangeLevel(crn, assessment, null, listOf(), listOf(conviction))

      assertThat(result.tier).isEqualTo(ChangeLevel.ONE)
      assertThat(result.points).isEqualTo(0)

      verify { assessmentApiService.getAssessmentNeeds(assessment.assessmentId) }
    }
  }
}
