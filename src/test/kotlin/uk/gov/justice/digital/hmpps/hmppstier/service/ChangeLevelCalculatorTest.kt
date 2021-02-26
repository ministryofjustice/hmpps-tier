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
import uk.gov.justice.digital.hmpps.hmppstier.client.Sentence
import uk.gov.justice.digital.hmpps.hmppstier.client.SentenceType
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

/*
  @Nested
  @DisplayName("Simple Recent Assessment tests")
  inner class SimpleRecentAssessmentTests {

    @Test
    fun `should not calculate change tier if not recent`() {
      setUpValidResponses(false)
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.tierDto.changeLevel).isEqualTo(ChangeLevel.TWO)
      assertThat(tier.tierDto.changePoints).isEqualTo(0)
    }

    private fun setUpValidResponses(recent: Boolean) {
      every { assessmentApiService.isAssessmentRecent(crn) } returns recent
      every { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) } returns null
      every { communityApiDataService.hasCurrentCustodialSentence(crn) } returns true
      every { communityApiDataService.isFemaleOffender(crn) } returns false
      every { communityApiDataService.getRosh(crn) } returns null
      every { communityApiDataService.getMappa(crn) } returns null
      every { communityApiDataService.getComplexityFactors(crn) } returns listOf()
      every { communityApiDataService.getRSR(crn) } returns null

      val slot = slot<TierCalculationEntity>()
      every { tierCalculationRepository.save(capture(slot)) } answers { slot.captured }
    }

    private fun standardVerify() {
      verify { assessmentApiService.isAssessmentRecent(crn) }
      verify { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) }
      verify { communityApiDataService.hasCurrentCustodialSentence(crn) }
      verify { communityApiDataService.isFemaleOffender(crn) }
      verify { communityApiDataService.getRosh(crn) }
      verify { communityApiDataService.getMappa(crn) }
      verify { communityApiDataService.getComplexityFactors(crn) }
      verify { communityApiDataService.getRSR(crn) }
      verify { tierCalculationRepository.save(any()) }
    }
  }

  @Nested
  @DisplayName("Simple Mandate for change tests")
  inner class SimpleMandateForChangeTests {

    @Test
    fun `should not calculate change tier if current noncustodial, no restrictive requirements and some unpaid work`() {
      setUpValidNonCustodialResponses(currentCustodial = false, currentNonCustodial = true, restrictiveRequirements = false, unpaidWork = true)
      val tier = service.calculateTierForCrn(crn)
      nonCustodialVerify()
      verify { communityApiDataService.hasUnpaidWork(crn) }

      assertThat(tier.tierDto.changeLevel).isEqualTo(ChangeLevel.ZERO)
      assertThat(tier.tierDto.changePoints).isEqualTo(0)
    }

    @Test
    fun `should not calculate change tier if current noncustodial, with restrictive requirements`() {
      setUpValidNonCustodialResponses(currentCustodial = false, currentNonCustodial = true, restrictiveRequirements = true, unpaidWork = false)
      val tier = service.calculateTierForCrn(crn)
      nonCustodialVerify()

      assertThat(tier.tierDto.changeLevel).isEqualTo(ChangeLevel.ZERO)
      assertThat(tier.tierDto.changePoints).isEqualTo(0)
    }

    @Test
    fun `should calculate change tier if current noncustodial, with no restrictive requirements or unpaidworkd`() {
      setUpValidResponses(currentCustodial = false)
      every { communityApiDataService.hasCurrentNonCustodialSentence(crn) } returns true
      every { communityApiDataService.hasRestrictiveRequirements(crn) } returns false
      every { communityApiDataService.hasUnpaidWork(crn) } returns false
      val tier = service.calculateTierForCrn(crn)
      standardVerify()
      verify { communityApiDataService.hasCurrentNonCustodialSentence(crn) }
      verify { communityApiDataService.hasRestrictiveRequirements(crn) }
      verify { communityApiDataService.hasUnpaidWork(crn) }

      assertThat(tier.tierDto.changeLevel).isEqualTo(ChangeLevel.ONE)
      assertThat(tier.tierDto.changePoints).isEqualTo(0)
    }

    @Test
    fun `should calculate change tier if current custodial`() {
      setUpValidResponses(currentCustodial = true)
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.tierDto.changeLevel).isEqualTo(ChangeLevel.ONE)
      assertThat(tier.tierDto.changePoints).isEqualTo(0)
    }

    private fun setUpValidNonCustodialResponses(currentCustodial: Boolean, currentNonCustodial: Boolean, restrictiveRequirements: Boolean, unpaidWork: Boolean) {
      every { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) } returns null
      every { communityApiDataService.hasCurrentCustodialSentence(crn) } returns currentCustodial
      every { communityApiDataService.hasCurrentNonCustodialSentence(crn) } returns currentNonCustodial
      every { communityApiDataService.hasRestrictiveRequirements(crn) } returns restrictiveRequirements
      every { communityApiDataService.hasUnpaidWork(crn) } returns unpaidWork
      every { communityApiDataService.isFemaleOffender(crn) } returns false
      every { communityApiDataService.getRosh(crn) } returns null
      every { communityApiDataService.getMappa(crn) } returns null
      every { communityApiDataService.getComplexityFactors(crn) } returns listOf()
      every { communityApiDataService.getRSR(crn) } returns null

      val slot = slot<TierCalculationEntity>()
      every { tierCalculationRepository.save(capture(slot)) } answers { slot.captured }
    }

    private fun nonCustodialVerify() {
      verify { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) }
      verify { communityApiDataService.hasCurrentCustodialSentence(crn) }
      verify { communityApiDataService.hasCurrentNonCustodialSentence(crn) }
      verify { communityApiDataService.hasRestrictiveRequirements(crn) }
      verify { communityApiDataService.isFemaleOffender(crn) }
      verify { communityApiDataService.getRosh(crn) }
      verify { communityApiDataService.getMappa(crn) }
      verify { communityApiDataService.getComplexityFactors(crn) }
      verify { communityApiDataService.getRSR(crn) }
      verify { tierCalculationRepository.save(any()) }
    }

    private fun setUpValidResponses(currentCustodial: Boolean, isFemale: Boolean = true) {
      every { assessmentApiService.isAssessmentRecent(crn) } returns true
      every { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) } returns null
      every { communityApiDataService.hasCurrentCustodialSentence(crn) } returns currentCustodial
      every { communityApiDataService.isFemaleOffender(crn) } returns isFemale
      every { communityApiDataService.getRosh(crn) } returns null
      every { communityApiDataService.getMappa(crn) } returns null
      every { communityApiDataService.getComplexityFactors(crn) } returns listOf()
      every { assessmentApiService.getAssessmentNeeds(crn) } returns mapOf()
      every { communityApiDataService.getRSR(crn) } returns null
      every { communityApiDataService.getOGRS(crn) } returns null

      if (isFemale) {
        every { communityApiDataService.hasBreachedConvictions(crn) } returns false
        every { assessmentApiService.getAssessmentComplexityAnswers(crn) } returns mapOf()
      }

      val slot = slot<TierCalculationEntity>()
      every { tierCalculationRepository.save(capture(slot)) } answers { slot.captured }
    }

    private fun standardVerify(isFemale: Boolean = true) {
      verify { assessmentApiService.isAssessmentRecent(crn) }
      verify { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) }
      verify { communityApiDataService.hasCurrentCustodialSentence(crn) }
      verify { communityApiDataService.isFemaleOffender(crn) }
      verify { communityApiDataService.getRosh(crn) }
      verify { communityApiDataService.getMappa(crn) }
      verify { communityApiDataService.getComplexityFactors(crn) }
      verify { assessmentApiService.getAssessmentNeeds(crn) }
      verify { communityApiDataService.getRSR(crn) }
      verify { communityApiDataService.getOGRS(crn) }
      verify { tierCalculationRepository.save(any()) }

      if (isFemale) {
        verify { communityApiDataService.hasBreachedConvictions(crn) }
        verify { assessmentApiService.getAssessmentComplexityAnswers(crn) }
      }
    }
  }

  @Nested
  @DisplayName("Get Needs Tests")
  inner class GetNeedsTests {

    @Test
    fun `Should return empty Map if no Needs`() {
      val crn = "123"
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null)
      val needs = listOf<AssessmentNeed>()

      every { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment)
      every { assessmentApiClient.getAssessmentNeeds(assessment.assessmentId) } returns needs
      val returnValue = assessmentService.getAssessmentNeeds(crn)

      assertThat(returnValue).isEmpty()
    }

    @Test
    fun `Should return Needs`() {
      val crn = "123"
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null)
      val needs = listOf(
        AssessmentNeed(
          Need.ACCOMMODATION,
          NeedSeverity.NO_NEED
        )
      )

      every { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment)
      every { assessmentApiClient.getAssessmentNeeds(assessment.assessmentId) } returns needs
      val returnValue = assessmentService.getAssessmentNeeds(crn)

      assertThat(returnValue).hasSize(1)
      assertThat(returnValue).containsEntry(Need.ACCOMMODATION, NeedSeverity.NO_NEED)
    }
  }

  @Nested
  @DisplayName("Get recent Assessment Tests")
  inner class GetRecentAssessmentTests {

    @Test
    fun `Should return true if inside Threshold`() {
      val crn = "123"
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock).minusWeeks(55), null)

      every { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment)
      val returnValue = assessmentService.isAssessmentRecent(crn)

      assertThat(returnValue).isTrue
    }

    @Test
    fun `Should return false if outside Threshold`() {
      val crn = "123"
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock).minusWeeks(55).minusDays(1), null)
      // more recent, but voided
      val voidedAssessment = OffenderAssessment("1234", LocalDateTime.now(clock).minusWeeks(40), LocalDateTime.now(clock))

      every { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment, voidedAssessment)
      val returnValue = assessmentService.isAssessmentRecent(crn)

      assertThat(returnValue).isFalse
    }

    @Test
    fun `Should throw if none valid`() {
      val crn = "123"

      every { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf()

      assertThrows(EntityNotFoundException::class.java) {
        assessmentService.isAssessmentRecent(crn)
      }
    }

    @Test
    fun `Should throw if none valid with entries`() {
      val crn = "123"
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock).minusWeeks(55).minusDays(1), LocalDateTime.now(clock))

      every { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment)

      assertThrows(EntityNotFoundException::class.java) {
        assessmentService.isAssessmentRecent(crn)
      }
    }
  }*/
}
