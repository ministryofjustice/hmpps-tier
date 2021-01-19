package uk.gov.justice.digital.hmpps.hmppstier.service

import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.RsrThresholds

@ExtendWith(MockKExtension::class)
@DisplayName("Tier Calculation Service tests")
internal class TierCalculationServiceTest {

  private val communityApiDataService: CommunityApiDataService = mockk(relaxUnitFun = true)
  private val assessmentApiDataService: AssessmentApiDataService = mockk(relaxUnitFun = true)
  private val tierCalculationRepository: TierCalculationRepository = mockk(relaxUnitFun = true)
  private val clock =
    Clock.fixed(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())

  private val service = TierCalculationService(
    communityApiDataService,
    assessmentApiDataService,
    tierCalculationRepository,
    clock
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
      every { communityApiDataService.isFemaleOffender(crn) } returns true
      every { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) } returns null
      every { communityApiDataService.getRosh(crn) } returns Rosh.MEDIUM
      every { communityApiDataService.getMappa(crn) } returns Mappa.M3
      every { communityApiDataService.getComplexityFactors(crn) } returns listOf()
      every { assessmentApiDataService.getAssessmentComplexityAnswers(crn) } returns mapOf()
      every { assessmentApiDataService.getAssessmentNeeds(crn) } returns mapOf()
      every { communityApiDataService.getRSR(crn) } returns BigDecimal(3)
      every { communityApiDataService.getOGRS(crn) } returns 55

      every { tierCalculationRepository.save(any()) } returns validTierCalculationEntity

      service.getTierByCrn(crn)

      verify { communityApiDataService.isFemaleOffender(crn) }
      verify { communityApiDataService.getRosh(crn) }
      verify { communityApiDataService.getMappa(crn) }
      verify { communityApiDataService.getComplexityFactors(crn) }
      verify { assessmentApiDataService.getAssessmentComplexityAnswers(crn) }
      verify { assessmentApiDataService.getAssessmentNeeds(crn) }
      verify { communityApiDataService.getRSR(crn) }
      verify { communityApiDataService.getOGRS(crn) }
      verify { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) }
      verify { tierCalculationRepository.save(any()) }
    }

    @Test
    fun `Should Call Collaborators Test - Male - Existing not found`() {
      // no call to AssessmentComplexity
      every { communityApiDataService.isFemaleOffender(crn) } returns false
      every { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) } returns null
      every { communityApiDataService.getRosh(crn) } returns Rosh.MEDIUM
      every { communityApiDataService.getMappa(crn) } returns Mappa.M3
      every { communityApiDataService.getComplexityFactors(crn) } returns listOf()
      every { assessmentApiDataService.getAssessmentNeeds(crn) } returns mapOf()
      every { communityApiDataService.getRSR(crn) } returns BigDecimal(3)
      every { communityApiDataService.getOGRS(crn) } returns 55

      every { tierCalculationRepository.save(any()) } returns validTierCalculationEntity

      service.getTierByCrn(crn)

      verify { communityApiDataService.isFemaleOffender(crn) }
      verify { communityApiDataService.getRosh(crn) }
      verify { communityApiDataService.getMappa(crn) }
      verify { communityApiDataService.getComplexityFactors(crn) }
      verify { assessmentApiDataService.getAssessmentNeeds(crn) }
      verify { communityApiDataService.getRSR(crn) }
      verify { communityApiDataService.getOGRS(crn) }
      verify { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) }
      verify { tierCalculationRepository.save(any()) }
    }

    @Test
    fun `Should Call Collaborators Test - Existing found`() {
      every { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) } returns validTierCalculationEntity

      service.getTierByCrn(crn)

      verify { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) }
    }
  }

  @Nested
  @DisplayName("Simple Risk tests")
  inner class SimpleRiskTests {
    @Test
    fun `should use RSR when higher than ROSH`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = RsrThresholds.TIER_B_RSR.num.plus(BigDecimal(1)),
        roshScore = Rosh.MEDIUM,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores, true)

      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.RSR_USED_OVER_ROSH)
    }

    @Test
    fun `should use ROSH when higher than RSR`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = RsrThresholds.TIER_B_RSR.num,
        roshScore = Rosh.VERY_HIGH,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores, true)

      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.ROSH_USED_OVER_RSR)
    }

    @Test
    fun `should use either when RSR is same as ROSH`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = RsrThresholds.TIER_C_RSR.num,
        roshScore = Rosh.MEDIUM,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores, true)

      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.RSR_ROSH_EQUAL)
    }

    @Test
    fun `should include ROSH or RSR when male`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = RsrThresholds.TIER_B_RSR.num,
        roshScore = Rosh.VERY_HIGH,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores, false)

      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.ROSH_USED_OVER_RSR)
    }
  }
}
