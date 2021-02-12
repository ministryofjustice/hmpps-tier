package uk.gov.justice.digital.hmpps.hmppstier.service

import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AssessmentComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.RsrThresholds
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@ExtendWith(MockKExtension::class)
@DisplayName("Tier Calculation Service tests")
internal class TierCalculationServiceTest {

  private val communityApiDataService: CommunityApiDataService = mockk(relaxUnitFun = true)
  private val assessmentApiDataService: AssessmentApiDataService = mockk(relaxUnitFun = true)
  private val tierCalculationRepository: TierCalculationRepository = mockk(relaxUnitFun = true)
  private val changeLevelCalculator: ChangeLevelCalculator = ChangeLevelCalculator(communityApiDataService, assessmentApiDataService)
  private val protectLevelCalculator: ProtectLevelCalculator = ProtectLevelCalculator(communityApiDataService, assessmentApiDataService)
  private val clock = Clock.fixed(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())

  private val service = TierCalculationService(
    tierCalculationRepository,
    clock,
    changeLevelCalculator,
    protectLevelCalculator
  )

  private val crn = "Any Crn"
  private val tierLetterResult = TierLevel(ProtectLevel.B, 0)
  private val tierNumberResult = TierLevel(ChangeLevel.TWO, 0)
  private val validTierCalculationEntity = TierCalculationEntity(
    0,
    crn,
    LocalDateTime.now(clock),
    TierCalculationResultEntity(tierLetterResult, tierNumberResult)
  )

  @BeforeEach
  fun resetAllMocks() {
    clearMocks(communityApiDataService)
    clearMocks(assessmentApiDataService)
    clearMocks(tierCalculationRepository)
  }

  @AfterEach
  fun confirmVerified() {
    // Check we don't add any more calls without updating the tests
    confirmVerified(communityApiDataService)
    confirmVerified(assessmentApiDataService)
    confirmVerified(tierCalculationRepository)
  }

  @Nested
  @DisplayName("Get Tier By Crn tests")
  inner class GetTierByCrnTests {

    @Test
    fun `Should Call Collaborators Test - Female - Existing not found`() {
      every { communityApiDataService.hasCurrentCustodialSentence(crn) } returns true
      every { communityApiDataService.isFemaleOffender(crn) } returns true
      every { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) } returns null
      every { communityApiDataService.getRosh(crn) } returns Rosh.MEDIUM
      every { communityApiDataService.getMappa(crn) } returns Mappa.M3
      every { communityApiDataService.getComplexityFactors(crn) } returns listOf()
      every { assessmentApiDataService.getAssessmentComplexityAnswers(crn) } returns mapOf()
      every { assessmentApiDataService.getAssessmentNeeds(crn) } returns mapOf()
      every { communityApiDataService.getRSR(crn) } returns BigDecimal(3)
      every { communityApiDataService.getOGRS(crn) } returns 55
      every { communityApiDataService.hasBreachedConvictions(crn) } returns false
      every { assessmentApiDataService.isLatestAssessmentRecent(crn) } returns true

      every { tierCalculationRepository.save(any()) } returns validTierCalculationEntity

      service.getTierByCrn(crn)

      verify { communityApiDataService.hasCurrentCustodialSentence(crn) }
      verify { communityApiDataService.isFemaleOffender(crn) }
      verify { communityApiDataService.getRosh(crn) }
      verify { communityApiDataService.getMappa(crn) }
      verify { communityApiDataService.getComplexityFactors(crn) }
      verify { assessmentApiDataService.getAssessmentComplexityAnswers(crn) }
      verify { assessmentApiDataService.getAssessmentNeeds(crn) }
      verify { communityApiDataService.getRSR(crn) }
      verify { communityApiDataService.getOGRS(crn) }
      verify { communityApiDataService.hasBreachedConvictions(crn) }
      verify { assessmentApiDataService.isLatestAssessmentRecent(crn) }
      verify { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) }
      verify { tierCalculationRepository.save(any()) }
    }

    @Test
    fun `Should Call Collaborators Test - Male - Existing not found`() {
      // no call to AssessmentComplexity
      every { communityApiDataService.hasCurrentCustodialSentence(crn) } returns true
      every { communityApiDataService.isFemaleOffender(crn) } returns false
      every { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) } returns null
      every { communityApiDataService.getRosh(crn) } returns Rosh.MEDIUM
      every { communityApiDataService.getMappa(crn) } returns Mappa.M3
      every { communityApiDataService.getComplexityFactors(crn) } returns listOf()
      every { assessmentApiDataService.getAssessmentNeeds(crn) } returns mapOf()
      every { communityApiDataService.getRSR(crn) } returns BigDecimal(3)
      every { communityApiDataService.getOGRS(crn) } returns 55
      every { tierCalculationRepository.save(any()) } returns validTierCalculationEntity
      every { assessmentApiDataService.isLatestAssessmentRecent(crn) } returns true

      service.getTierByCrn(crn)

      verify { communityApiDataService.hasCurrentCustodialSentence(crn) }

      verify { communityApiDataService.isFemaleOffender(crn) }
      verify { communityApiDataService.getRosh(crn) }
      verify { communityApiDataService.getMappa(crn) }
      verify { communityApiDataService.getComplexityFactors(crn) }
      verify { assessmentApiDataService.getAssessmentNeeds(crn) }
      verify { communityApiDataService.getRSR(crn) }
      verify { communityApiDataService.getOGRS(crn) }
      verify { assessmentApiDataService.isLatestAssessmentRecent(crn) }
      verify { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) }
      verify { tierCalculationRepository.save(any()) }
    }

    @Test
    fun `Should Call Collaborators Test - Existing found`() {
      every { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) } returns validTierCalculationEntity
      every { tierCalculationRepository.save(any()) } returns validTierCalculationEntity
      service.getTierByCrn(crn)

      verify { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) }
    }
  }

  @Nested
  @DisplayName("Simple Risk tests")
  inner class SimpleRiskTests {

    @Test
    fun `should use RSR when higher than ROSH`() {
      // rsr B+1 = 20 points, Rosh.Medium = 10 Points
      setUpValidResponses(RsrThresholds.TIER_B_RSR.num.plus(BigDecimal(1)), Rosh.MEDIUM)
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(20)
    }

    @Test
    fun `should use ROSH when higher than RSR`() {
      // rsr B+1 = 20 points, Rosh.VeryHigh = 30 Points
      setUpValidResponses(RsrThresholds.TIER_B_RSR.num.plus(BigDecimal(1)), Rosh.VERY_HIGH)
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(30)
    }

    @Test
    fun `should use either when RSR is same as ROSH`() {
      // rsr C+1 = 10 points, Rosh.Medium = 10 Points
      setUpValidResponses(RsrThresholds.TIER_C_RSR.num.plus(BigDecimal(1)), Rosh.MEDIUM)
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(10)
    }

    @Test
    fun `should include ROSH or RSR when male`() {
      val isFemale = false
      // rsr B+1 = 20 points, Rosh.VeryHigh = 30 Points
      setUpValidResponses(RsrThresholds.TIER_B_RSR.num.plus(BigDecimal(1)), Rosh.VERY_HIGH, isFemale)
      val tier = service.calculateTierForCrn(crn)
      standardVerify(isFemale)

      assertThat(tier.data.protect.points).isEqualTo(30)
    }

    private fun setUpValidResponses(rsr: BigDecimal, rosh: Rosh, isFemale: Boolean = true) {
      every { assessmentApiDataService.isLatestAssessmentRecent(crn) } returns true
      every { communityApiDataService.hasCurrentCustodialSentence(crn) } returns true
      every { communityApiDataService.isFemaleOffender(crn) } returns isFemale
      every { communityApiDataService.getRosh(crn) } returns rosh
      every { communityApiDataService.getMappa(crn) } returns null
      every { communityApiDataService.getComplexityFactors(crn) } returns listOf()
      every { assessmentApiDataService.getAssessmentNeeds(crn) } returns mapOf()
      every { communityApiDataService.getRSR(crn) } returns rsr
      every { communityApiDataService.getOGRS(crn) } returns null

      if (isFemale) {
        every { communityApiDataService.hasBreachedConvictions(crn) } returns false
        every { assessmentApiDataService.getAssessmentComplexityAnswers(crn) } returns mapOf()
      }

      val slot = slot<TierCalculationEntity>()
      every { tierCalculationRepository.save(capture(slot)) } answers { slot.captured }
    }

    private fun standardVerify(isFemale: Boolean = true) {
      verify { assessmentApiDataService.isLatestAssessmentRecent(crn) }
      verify { communityApiDataService.hasCurrentCustodialSentence(crn) }
      verify { communityApiDataService.isFemaleOffender(crn) }
      verify { communityApiDataService.getRosh(crn) }
      verify { communityApiDataService.getMappa(crn) }
      verify { communityApiDataService.getComplexityFactors(crn) }
      verify { assessmentApiDataService.getAssessmentNeeds(crn) }
      verify { communityApiDataService.getRSR(crn) }
      verify { communityApiDataService.getOGRS(crn) }
      verify { tierCalculationRepository.save(any()) }

      if (isFemale) {
        verify { communityApiDataService.hasBreachedConvictions(crn) }
        verify { assessmentApiDataService.getAssessmentComplexityAnswers(crn) }
      }
    }
  }

  @Nested
  @DisplayName("Simple RSR tests")
  inner class SimpleRSRTests {

    @Test
    fun `should return 20 for RSR equal to tier B`() {
      setUpValidResponses(RsrThresholds.TIER_B_RSR.num)
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(20)
    }

    @Test
    fun `should return 20 for RSR greater than tier B`() {
      setUpValidResponses(RsrThresholds.TIER_B_RSR.num.plus(BigDecimal(1)))
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(20)
    }

    @Test
    fun `should return 10 for RSR equal to tier C`() {

      setUpValidResponses(RsrThresholds.TIER_C_RSR.num)
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(10)
    }

    @Test
    fun `should return 10 for RSR greater than tier C`() {
      setUpValidResponses(RsrThresholds.TIER_C_RSR.num.plus(BigDecimal(1)))
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(10)
    }

    @Test
    fun `should return 0 for RSR less than tier C`() {
      setUpValidResponses(RsrThresholds.TIER_C_RSR.num.minus(BigDecimal(1)))
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(0)
    }

    @Test
    fun `should return 0 for RSR null`() {
      setUpValidResponses(null)
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(0)
    }

    private fun setUpValidResponses(rsr: BigDecimal?, isFemale: Boolean = true) {
      every { assessmentApiDataService.isLatestAssessmentRecent(crn) } returns true
      every { communityApiDataService.hasCurrentCustodialSentence(crn) } returns true
      every { communityApiDataService.isFemaleOffender(crn) } returns isFemale
      every { communityApiDataService.getRosh(crn) } returns null
      every { communityApiDataService.getMappa(crn) } returns null
      every { communityApiDataService.getComplexityFactors(crn) } returns listOf()
      every { assessmentApiDataService.getAssessmentNeeds(crn) } returns mapOf()
      every { communityApiDataService.getRSR(crn) } returns rsr
      every { communityApiDataService.getOGRS(crn) } returns null

      if (isFemale) {
        every { communityApiDataService.hasBreachedConvictions(crn) } returns false
        every { assessmentApiDataService.getAssessmentComplexityAnswers(crn) } returns mapOf()
      }

      val slot = slot<TierCalculationEntity>()
      every { tierCalculationRepository.save(capture(slot)) } answers { slot.captured }
    }

    private fun standardVerify(isFemale: Boolean = true) {
      verify { assessmentApiDataService.isLatestAssessmentRecent(crn) }
      verify { communityApiDataService.hasCurrentCustodialSentence(crn) }
      verify { communityApiDataService.isFemaleOffender(crn) }
      verify { communityApiDataService.getRosh(crn) }
      verify { communityApiDataService.getMappa(crn) }
      verify { communityApiDataService.getComplexityFactors(crn) }
      verify { assessmentApiDataService.getAssessmentNeeds(crn) }
      verify { communityApiDataService.getRSR(crn) }
      verify { communityApiDataService.getOGRS(crn) }
      verify { tierCalculationRepository.save(any()) }

      if (isFemale) {
        verify { communityApiDataService.hasBreachedConvictions(crn) }
        verify { assessmentApiDataService.getAssessmentComplexityAnswers(crn) }
      }
    }
  }

  @Nested
  @DisplayName("Simple ROSH tests")
  inner class SimpleRoshTests {

    @Test
    fun `should return 30 for Very High Rosh`() {
      setUpValidResponses(Rosh.VERY_HIGH)
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(30)
    }

    @Test
    fun `should return 30 for High Rosh`() {
      setUpValidResponses(Rosh.HIGH)
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(20)
    }

    @Test
    fun `should return 10 for Medium Rosh`() {
      setUpValidResponses(Rosh.MEDIUM)
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(10)
    }

    @Test
    fun `should return 0 for No Rosh`() {
      setUpValidResponses(null)
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(0)
    }

    private fun setUpValidResponses(rosh: Rosh?, isFemale: Boolean = true) {
      every { assessmentApiDataService.isLatestAssessmentRecent(crn) } returns true
      every { communityApiDataService.hasCurrentCustodialSentence(crn) } returns true
      every { communityApiDataService.isFemaleOffender(crn) } returns isFemale
      every { communityApiDataService.getRosh(crn) } returns rosh
      every { communityApiDataService.getMappa(crn) } returns null
      every { communityApiDataService.getComplexityFactors(crn) } returns listOf()
      every { assessmentApiDataService.getAssessmentNeeds(crn) } returns mapOf()
      every { communityApiDataService.getRSR(crn) } returns null
      every { communityApiDataService.getOGRS(crn) } returns null

      if (isFemale) {
        every { communityApiDataService.hasBreachedConvictions(crn) } returns false
        every { assessmentApiDataService.getAssessmentComplexityAnswers(crn) } returns mapOf()
      }

      val slot = slot<TierCalculationEntity>()
      every { tierCalculationRepository.save(capture(slot)) } answers { slot.captured }
    }

    private fun standardVerify(isFemale: Boolean = true) {
      verify { assessmentApiDataService.isLatestAssessmentRecent(crn) }
      verify { communityApiDataService.hasCurrentCustodialSentence(crn) }
      verify { communityApiDataService.isFemaleOffender(crn) }
      verify { communityApiDataService.getRosh(crn) }
      verify { communityApiDataService.getMappa(crn) }
      verify { communityApiDataService.getComplexityFactors(crn) }
      verify { assessmentApiDataService.getAssessmentNeeds(crn) }
      verify { communityApiDataService.getRSR(crn) }
      verify { communityApiDataService.getOGRS(crn) }
      verify { communityApiDataService.hasBreachedConvictions(crn) }
      verify { tierCalculationRepository.save(any()) }

      if (isFemale) {
        verify { communityApiDataService.hasBreachedConvictions(crn) }
        verify { assessmentApiDataService.getAssessmentComplexityAnswers(crn) }
      }
    }
  }

  @Nested
  @DisplayName("Simple Mappa tests")
  inner class SimpleMappaTests {

    @Test
    fun `should return 30 for Mappa level 3`() {
      setUpValidResponses(Mappa.M3)
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(30)
    }

    @Test
    fun `should return 30 for Mappa level 2`() {
      setUpValidResponses(Mappa.M2)
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(30)
    }

    @Test
    fun `should return 5 for Mappa level 1`() {
      setUpValidResponses(Mappa.M1)
      val tier = service.calculateTierForCrn(crn)

      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(5)
    }

    @Test
    fun `should return 0 for Mappa null`() {
      setUpValidResponses(null)
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(0)
    }

    @Test
    fun `should include Mappa when male`() {
      setUpValidResponses(Mappa.M1, false)
      val tier = service.calculateTierForCrn(crn)
      standardVerify(false)

      assertThat(tier.data.protect.points).isEqualTo(5)
    }

    private fun setUpValidResponses(mappa: Mappa?, isFemale: Boolean = true) {
      every { assessmentApiDataService.isLatestAssessmentRecent(crn) } returns true
      every { communityApiDataService.hasCurrentCustodialSentence(crn) } returns true
      every { communityApiDataService.isFemaleOffender(crn) } returns isFemale
      every { communityApiDataService.getRosh(crn) } returns null
      every { communityApiDataService.getMappa(crn) } returns mappa
      every { communityApiDataService.getComplexityFactors(crn) } returns listOf()
      every { assessmentApiDataService.getAssessmentNeeds(crn) } returns mapOf()
      every { communityApiDataService.getRSR(crn) } returns null
      every { communityApiDataService.getOGRS(crn) } returns null

      if (isFemale) {
        every { communityApiDataService.hasBreachedConvictions(crn) } returns false
        every { assessmentApiDataService.getAssessmentComplexityAnswers(crn) } returns mapOf()
      }

      val slot = slot<TierCalculationEntity>()
      every { tierCalculationRepository.save(capture(slot)) } answers { slot.captured }
    }

    private fun standardVerify(isFemale: Boolean = true) {
      verify { assessmentApiDataService.isLatestAssessmentRecent(crn) }
      verify { communityApiDataService.hasCurrentCustodialSentence(crn) }
      verify { communityApiDataService.isFemaleOffender(crn) }
      verify { communityApiDataService.getRosh(crn) }
      verify { communityApiDataService.getMappa(crn) }
      verify { communityApiDataService.getComplexityFactors(crn) }
      verify { assessmentApiDataService.getAssessmentNeeds(crn) }
      verify { communityApiDataService.getRSR(crn) }
      verify { communityApiDataService.getOGRS(crn) }
      verify { tierCalculationRepository.save(any()) }

      if (isFemale) {
        verify { communityApiDataService.hasBreachedConvictions(crn) }
        verify { assessmentApiDataService.getAssessmentComplexityAnswers(crn) }
      }
    }
  }

  @Nested
  @DisplayName("Simple Complexity tests")
  inner class SimpleComplexityTests {

    @Test
    fun `should not count complexity factors none`() {
      setUpValidResponses(listOf())
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(0)
    }

    @Test
    fun `should not count complexity factors duplicates`() {
      setUpValidResponses(
        listOf(
          ComplexityFactor.VULNERABILITY_ISSUE,
          ComplexityFactor.VULNERABILITY_ISSUE,
          ComplexityFactor.VULNERABILITY_ISSUE,
          ComplexityFactor.VULNERABILITY_ISSUE,
          ComplexityFactor.VULNERABILITY_ISSUE,
          ComplexityFactor.VULNERABILITY_ISSUE
        )
      )
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(2)
    }

    @Test
    fun `should not count assessment complexity factors duplicates`() {
      setUpValidResponses(
        listOf(),
        mapOf(
          AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "Y",
          AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "Y",
          AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "Y",
          AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "Y",
          AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "Y",
          AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "Y",
        )
      )

      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(2)
    }

    @Test
    fun `should combine complexity factors`() {
      setUpValidResponses(
        listOf(
          ComplexityFactor.VULNERABILITY_ISSUE,
        ),
        mapOf(
          AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "Y",
        )
      )

      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(4)
    }

    @Test
    fun `should add multiple complexity factors`() {
      setUpValidResponses(
        listOf(
          ComplexityFactor.VULNERABILITY_ISSUE,
          ComplexityFactor.ADULT_AT_RISK
        ),
        mapOf(
          AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "Y",
          AssessmentComplexityFactor.TEMPER_CONTROL to "1"
        )
      )

      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(8)
    }

    @Test
    fun `should count both Temper and Impulsivity as max '1'`() {
      setUpValidResponses(
        listOf(),
        mapOf(
          AssessmentComplexityFactor.TEMPER_CONTROL to "1",
          AssessmentComplexityFactor.IMPULSIVITY to "2",
        )
      )

      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(2) // 1 * 2 weighting for all complexity factors
    }

    @Test
    fun `should count Temper without Impulsivity as max '1'`() {
      setUpValidResponses(
        listOf(),
        mapOf(
          AssessmentComplexityFactor.TEMPER_CONTROL to "1",
        )
      )

      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(2) // 1 * 2 weighting for all complexity factors
    }

    @Test
    fun `should count Impulsivity without Temper as max '1'`() {
      setUpValidResponses(
        listOf(),
        mapOf(
          AssessmentComplexityFactor.IMPULSIVITY to "2",
        )
      )

      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(2) // 1 * 2 weighting for all complexity factors
    }

    @Test
    fun `should ignore negative Parenting`() {
      setUpValidResponses(
        listOf(),
        mapOf(
          AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "N",
        )
      )

      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(0)
    }

    @Test
    fun `should ignore negative Impulsivity`() {
      setUpValidResponses(
        listOf(),
        mapOf(
          AssessmentComplexityFactor.IMPULSIVITY to "0",
        )
      )

      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(0)
    }

    @Test
    fun `should ignore negative Temper`() {
      setUpValidResponses(
        listOf(),
        mapOf(
          AssessmentComplexityFactor.TEMPER_CONTROL to "0",
        )
      )

      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.protect.points).isEqualTo(0)
    }

    @Test
    fun `should not count Parenting if male`() {
      setUpValidResponses(
        listOf(),
        mapOf(
          AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "Y",
        ),
        false
      )

      val tier = service.calculateTierForCrn(crn)
      standardVerify(false)

      assertThat(tier.data.protect.points).isEqualTo(0)
    }

    @Test
    fun `should not count Impulsivity if male`() {
      setUpValidResponses(
        listOf(),
        mapOf(
          AssessmentComplexityFactor.IMPULSIVITY to "2",
        ),
        false
      )

      val tier = service.calculateTierForCrn(crn)
      standardVerify(false)

      assertThat(tier.data.protect.points).isEqualTo(0)
    }

    @Test
    fun `should not count Temper if male`() {
      setUpValidResponses(
        listOf(),
        mapOf(
          AssessmentComplexityFactor.TEMPER_CONTROL to "2",
        ),
        false
      )

      val tier = service.calculateTierForCrn(crn)
      standardVerify(false)

      assertThat(tier.data.protect.points).isEqualTo(0)
    }

    private fun setUpValidResponses(complexityFactors: List<ComplexityFactor>, assessmentComplexityFactors: Map<AssessmentComplexityFactor, String> = mapOf(), isFemale: Boolean = true) {
      every { assessmentApiDataService.isLatestAssessmentRecent(crn) } returns true
      every { communityApiDataService.hasCurrentCustodialSentence(crn) } returns true
      every { communityApiDataService.isFemaleOffender(crn) } returns isFemale
      every { communityApiDataService.getRosh(crn) } returns null
      every { communityApiDataService.getMappa(crn) } returns null
      every { communityApiDataService.getComplexityFactors(crn) } returns complexityFactors
      every { assessmentApiDataService.getAssessmentNeeds(crn) } returns mapOf()
      every { communityApiDataService.getRSR(crn) } returns null
      every { communityApiDataService.getOGRS(crn) } returns null

      if (isFemale) {
        every { communityApiDataService.hasBreachedConvictions(crn) } returns false
        every { assessmentApiDataService.getAssessmentComplexityAnswers(crn) } returns assessmentComplexityFactors
      }

      val slot = slot<TierCalculationEntity>()
      every { tierCalculationRepository.save(capture(slot)) } answers { slot.captured }
    }

    private fun standardVerify(isFemale: Boolean = true) {
      verify { assessmentApiDataService.isLatestAssessmentRecent(crn) }
      verify { communityApiDataService.hasCurrentCustodialSentence(crn) }
      verify { communityApiDataService.isFemaleOffender(crn) }
      verify { communityApiDataService.getRosh(crn) }
      verify { communityApiDataService.getMappa(crn) }
      verify { communityApiDataService.getComplexityFactors(crn) }
      verify { assessmentApiDataService.getAssessmentNeeds(crn) }
      verify { communityApiDataService.getRSR(crn) }
      verify { communityApiDataService.getOGRS(crn) }
      verify { tierCalculationRepository.save(any()) }

      if (isFemale) {
        verify { communityApiDataService.hasBreachedConvictions(crn) }
        verify { assessmentApiDataService.getAssessmentComplexityAnswers(crn) }
      }
    }
  }

  @Nested
  @DisplayName("Simple Ogrs tests")
  inner class SimpleOgrsTests {

    @Test
    fun `should calculate Ogrs null`() {
      setUpValidResponses(null)
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.change.points).isEqualTo(0)
    }

    @Test
    fun `should calculate Ogrs take 10s 50`() {
      setUpValidResponses(50)
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.change.points).isEqualTo(5)
    }

    @Test
    fun `should calculate Ogrs take 10s 51`() {
      setUpValidResponses(51)
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.change.points).isEqualTo(5)
    }

    @Test
    fun `should calculate Ogrs take 10s 59`() {
      setUpValidResponses(59)
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.change.points).isEqualTo(5)
    }

    @Test
    fun `should calculate Ogrs take 10s 50 if male`() {
      setUpValidResponses(50, false)
      val tier = service.calculateTierForCrn(crn)
      standardVerify(false)

      assertThat(tier.data.change.points).isEqualTo(5)
    }

    private fun setUpValidResponses(ogrs: Int?, isFemale: Boolean = true) {
      every { assessmentApiDataService.isLatestAssessmentRecent(crn) } returns true
      every { communityApiDataService.hasCurrentCustodialSentence(crn) } returns true
      every { communityApiDataService.isFemaleOffender(crn) } returns isFemale
      every { communityApiDataService.getRosh(crn) } returns null
      every { communityApiDataService.getMappa(crn) } returns null
      every { communityApiDataService.getComplexityFactors(crn) } returns listOf()
      every { assessmentApiDataService.getAssessmentNeeds(crn) } returns mapOf()
      every { communityApiDataService.getRSR(crn) } returns null
      every { communityApiDataService.getOGRS(crn) } returns ogrs

      if (isFemale) {
        every { communityApiDataService.hasBreachedConvictions(crn) } returns false
        every { assessmentApiDataService.getAssessmentComplexityAnswers(crn) } returns mapOf()
      }

      val slot = slot<TierCalculationEntity>()
      every { tierCalculationRepository.save(capture(slot)) } answers { slot.captured }
    }

    private fun standardVerify(isFemale: Boolean = true) {
      verify { assessmentApiDataService.isLatestAssessmentRecent(crn) }
      verify { communityApiDataService.hasCurrentCustodialSentence(crn) }
      verify { communityApiDataService.isFemaleOffender(crn) }
      verify { communityApiDataService.getRosh(crn) }
      verify { communityApiDataService.getMappa(crn) }
      verify { communityApiDataService.getComplexityFactors(crn) }
      verify { assessmentApiDataService.getAssessmentNeeds(crn) }
      verify { communityApiDataService.getRSR(crn) }
      verify { communityApiDataService.getOGRS(crn) }
      verify { tierCalculationRepository.save(any()) }

      if (isFemale) {
        verify { communityApiDataService.hasBreachedConvictions(crn) }
        verify { assessmentApiDataService.getAssessmentComplexityAnswers(crn) }
      }
    }
  }

  @Nested
  @DisplayName("Simple Oasys Needs tests")
  inner class SimpleNeedsTests {

    @Test
    fun `should calculate Oasys Needs none`() {
      setUpValidResponses(mapOf())
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.change.points).isEqualTo(0)
    }

    @Test
    fun `should add Oasys Needs no need`() {
      setUpValidResponses(
        mapOf(
          Need.ACCOMMODATION to NeedSeverity.NO_NEED, // 0
        ),
      )
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.change.points).isEqualTo(0)
    }

    @Test
    fun `should add Oasys Needs standard need`() {
      setUpValidResponses(
        mapOf(
          Need.ACCOMMODATION to NeedSeverity.STANDARD, // 1
        ),
      )
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.change.points).isEqualTo(1)
    }

    @Test
    fun `should add Oasys Needs severe need`() {
      setUpValidResponses(
        mapOf(
          Need.ACCOMMODATION to NeedSeverity.SEVERE, // 2
        ),
      )
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.change.points).isEqualTo(2)
    }

    @Test
    fun `should add multiple Oasys Needs`() {
      setUpValidResponses(
        mapOf(
          Need.ACCOMMODATION to NeedSeverity.SEVERE, // 2
          Need.EDUCATION_TRAINING_AND_EMPLOYABILITY to NeedSeverity.SEVERE, // 2
          Need.RELATIONSHIPS to NeedSeverity.SEVERE, // 2
          Need.LIFESTYLE_AND_ASSOCIATES to NeedSeverity.SEVERE, // 2
          Need.DRUG_MISUSE to NeedSeverity.SEVERE, // 2
        ),
      )
      val tier = service.calculateTierForCrn(crn)
      standardVerify()

      assertThat(tier.data.change.points).isEqualTo(10)
    }

    @Test
    fun `should add multiple Oasys Needs if Male`() {
      setUpValidResponses(
        mapOf(
          Need.ACCOMMODATION to NeedSeverity.SEVERE, // 2
          Need.EDUCATION_TRAINING_AND_EMPLOYABILITY to NeedSeverity.SEVERE, // 2
          Need.RELATIONSHIPS to NeedSeverity.SEVERE, // 2
          Need.LIFESTYLE_AND_ASSOCIATES to NeedSeverity.SEVERE, // 2
          Need.DRUG_MISUSE to NeedSeverity.SEVERE, // 2
        ),
        false
      )
      val tier = service.calculateTierForCrn(crn)
      standardVerify(false)

      assertThat(tier.data.change.points).isEqualTo(10)
    }

    private fun setUpValidResponses(needs: Map<Need, NeedSeverity?>, isFemale: Boolean = true) {
      every { assessmentApiDataService.isLatestAssessmentRecent(crn) } returns true
      every { communityApiDataService.hasCurrentCustodialSentence(crn) } returns true
      every { communityApiDataService.isFemaleOffender(crn) } returns isFemale
      every { communityApiDataService.getRosh(crn) } returns null
      every { communityApiDataService.getMappa(crn) } returns null
      every { communityApiDataService.getComplexityFactors(crn) } returns listOf()
      every { assessmentApiDataService.getAssessmentNeeds(crn) } returns needs
      every { communityApiDataService.getRSR(crn) } returns null
      every { communityApiDataService.getOGRS(crn) } returns null

      if (isFemale) {
        every { communityApiDataService.hasBreachedConvictions(crn) } returns false
        every { assessmentApiDataService.getAssessmentComplexityAnswers(crn) } returns mapOf()
      }

      val slot = slot<TierCalculationEntity>()
      every { tierCalculationRepository.save(capture(slot)) } answers { slot.captured }
    }

    private fun standardVerify(isFemale: Boolean = true) {
      verify { assessmentApiDataService.isLatestAssessmentRecent(crn) }
      verify { communityApiDataService.hasCurrentCustodialSentence(crn) }
      verify { communityApiDataService.isFemaleOffender(crn) }
      verify { communityApiDataService.getRosh(crn) }
      verify { communityApiDataService.getMappa(crn) }
      verify { communityApiDataService.getComplexityFactors(crn) }
      verify { assessmentApiDataService.getAssessmentNeeds(crn) }
      verify { communityApiDataService.getRSR(crn) }
      verify { communityApiDataService.getOGRS(crn) }
      verify { tierCalculationRepository.save(any()) }

      if (isFemale) {
        verify { communityApiDataService.hasBreachedConvictions(crn) }
        verify { assessmentApiDataService.getAssessmentComplexityAnswers(crn) }
      }
    }
  }
}
