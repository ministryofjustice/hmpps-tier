package uk.gov.justice.digital.hmpps.hmppstier.service

import io.mockk.*
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierCalculation
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierResult
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeScore
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectScore
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@ExtendWith(MockKExtension::class)
@DisplayName("Tier Calculation Service tests")
internal class TierCalculationServiceTest {

  private val tierCalculation = TierCalculation()
  private val communityApiDataService: CommunityApiDataService = mockk(relaxUnitFun = true)
  private val assessmentApiDataService: AssessmentApiDataService = mockk(relaxUnitFun = true)
  private val tierCalculationRepository: TierCalculationRepository = mockk(relaxUnitFun = true)
  private val clock =
    Clock.fixed(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())

  private val service = TierCalculationService(
    tierCalculation,
    communityApiDataService,
    assessmentApiDataService,
    tierCalculationRepository,
    clock
  )

  private val crn = "Any Crn"
  private val tierLetterResult = TierResult(ProtectScore.B, 0, setOf())
  private val tierNumberResult = TierResult(ChangeScore.TWO, 0, setOf())
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
    fun `Should Call Collaborators Test - Existing not found`() {
      every { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) } returns null
      every { communityApiDataService.getRosh(crn) } returns Rosh.MEDIUM
      every { communityApiDataService.getMappa(crn) } returns Mappa.M3
      every { communityApiDataService.getComplexityFactors(crn) } returns listOf()
      every { assessmentApiDataService.getAssessmentComplexityAnswers(crn) } returns listOf()
      every { assessmentApiDataService.getAssessmentNeeds(crn) } returns mapOf()

      every { tierCalculationRepository.save(any()) } returns validTierCalculationEntity

      service.getTierByCrn(crn)

      verify { communityApiDataService.getRosh(crn) }
      verify { communityApiDataService.getMappa(crn) }
      verify { communityApiDataService.getComplexityFactors(crn) }
      verify { assessmentApiDataService.getAssessmentComplexityAnswers(crn) }
      verify { assessmentApiDataService.getAssessmentNeeds(crn) }
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
}