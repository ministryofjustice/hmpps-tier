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
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

@ExtendWith(MockKExtension::class)
@DisplayName("Tier Calculation Service tests")
internal class TierCalculationServiceTest {

  private val clock = Clock.fixed(LocalDateTime.of(2020, 1, 1, 0, 0).toInstant(ZoneOffset.UTC), ZoneId.systemDefault())

  private val tierCalculationRepository: TierCalculationRepository = mockk(relaxUnitFun = true)
  private val changeLevelCalculator: ChangeLevelCalculator = mockk(relaxUnitFun = true)
  private val protectLevelCalculator: ProtectLevelCalculator = mockk(relaxUnitFun = true)
  private val assessmentApiService: AssessmentApiService = mockk(relaxUnitFun = true)
  private val communityApiClient: CommunityApiClient = mockk(relaxUnitFun = true)

  private val service = TierCalculationService(
    clock,
    tierCalculationRepository,
    changeLevelCalculator,
    protectLevelCalculator,
    assessmentApiService,
    communityApiClient
  )

  private val calculationId = UUID.randomUUID()
  private val crn = "Any Crn"
  private val protectLevelResult = TierLevel(ProtectLevel.B, 0)
  private val changeLevelResult = TierLevel(ChangeLevel.TWO, 0)
  private val validTierCalculationEntity = TierCalculationEntity(
    0,
    calculationId,
    crn,
    LocalDateTime.now(clock),
    TierCalculationResultEntity(protectLevelResult, changeLevelResult)
  )

  @BeforeEach
  fun resetAllMocks() {
    clearMocks(tierCalculationRepository)
    clearMocks(changeLevelCalculator)
    clearMocks(protectLevelCalculator)
    clearMocks(assessmentApiService)
    clearMocks(communityApiClient)
  }

  @AfterEach
  fun confirmVerified() {
    // Check we don't add any more calls without updating the tests
    confirmVerified(tierCalculationRepository)
    confirmVerified(changeLevelCalculator)
    confirmVerified(protectLevelCalculator)
    confirmVerified(assessmentApiService)
    confirmVerified(communityApiClient)
  }

  @Nested
  @DisplayName("Get Tier By Crn tests")
  inner class GetTierByCrnTests {

    @Test
    fun `Should Call Collaborators Test - Existing found`() {
      every { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) } returns validTierCalculationEntity
      val result = service.getLatestTierByCrn(crn)

      assertThat(result?.protectLevel).isEqualTo(validTierCalculationEntity.data.protect.tier)
      assertThat(result?.protectPoints).isEqualTo(validTierCalculationEntity.data.protect.points)
      assertThat(result?.changeLevel).isEqualTo(validTierCalculationEntity.data.change.tier)
      assertThat(result?.changePoints).isEqualTo(validTierCalculationEntity.data.change.points)

      verify { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) }
    }

    @Test
    fun `Should Call Collaborators Test - Existing Not found`() {
      every { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) } returns null
      val result = service.getLatestTierByCrn(crn)

      assertThat(result).isNull()

      verify { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) }
    }
  }

  @Nested
  @DisplayName("Get Tier By Crn tests")
  inner class GetTierByCalculationIdTests {

    @Test
    fun `Should Call Collaborators Test - Existing found`() {
      every { tierCalculationRepository.findByCrnAndUuid(crn, calculationId) } returns validTierCalculationEntity
      val result = service.getTierByCalculationId(crn, calculationId)

      assertThat(result?.protectLevel).isEqualTo(validTierCalculationEntity.data.protect.tier)
      assertThat(result?.protectPoints).isEqualTo(validTierCalculationEntity.data.protect.points)
      assertThat(result?.changeLevel).isEqualTo(validTierCalculationEntity.data.change.tier)
      assertThat(result?.changePoints).isEqualTo(validTierCalculationEntity.data.change.points)
      assertThat(result?.calculationId).isEqualTo(validTierCalculationEntity.uuid)

      verify { tierCalculationRepository.findByCrnAndUuid(crn, calculationId) }
    }

    @Test
    fun `Should Call Collaborators Test - Existing Not found`() {
      every { tierCalculationRepository.findByCrnAndUuid(crn, calculationId) } returns null
      val result = service.getTierByCalculationId(crn, calculationId)

      assertThat(result).isNull()

      verify { tierCalculationRepository.findByCrnAndUuid(crn, calculationId) }
    }
  }

  @Nested
  @DisplayName("Calculate  Tier for Crn tests")
  inner class CalculateTierForCrnTests {

    @Test
    fun `Should Call Collaborators Test value not changed`() {
      every { assessmentApiService.getRecentAssessment(crn) } returns null // anything
      every { communityApiClient.getDeliusAssessments(crn) } returns null // anything
      every { communityApiClient.getRegistrations(crn) } returns listOf() // anything
      every { communityApiClient.getConvictions(crn) } returns listOf() // anything

      every { protectLevelCalculator.calculateProtectLevel(crn, any(), any(), any(), any()) } returns protectLevelResult
      every { changeLevelCalculator.calculateChangeLevel(crn, any(), any(), any(), any()) } returns changeLevelResult

      every { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) } returns validTierCalculationEntity

      val slot = slot<TierCalculationEntity>()
      every { tierCalculationRepository.save(capture(slot)) } answers { slot.captured }

      val result = service.calculateTierForCrn(crn)

      assertThat(result.tierDto.protectLevel).isEqualTo(slot.captured.data.protect.tier)
      assertThat(result.tierDto.protectPoints).isEqualTo(slot.captured.data.protect.points)
      assertThat(result.tierDto.changeLevel).isEqualTo(slot.captured.data.change.tier)
      assertThat(result.tierDto.changePoints).isEqualTo(slot.captured.data.change.points)

      assertThat(result.isUpdated).isFalse

      verify { assessmentApiService.getRecentAssessment(crn) }
      verify { communityApiClient.getDeliusAssessments(crn) }
      verify { communityApiClient.getRegistrations(crn) }
      verify { communityApiClient.getConvictions(crn) }
      verify { protectLevelCalculator.calculateProtectLevel(crn, any(), any(), any(), any()) }
      verify { changeLevelCalculator.calculateChangeLevel(crn, any(), any(), any(), any()) }
      verify { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) }
      verify { tierCalculationRepository.save(capture(slot)) }
    }

    @Test
    fun `Should Call Collaborators Test value changed`() {
      every { assessmentApiService.getRecentAssessment(crn) } returns null // anything
      every { communityApiClient.getDeliusAssessments(crn) } returns null // anything
      every { communityApiClient.getRegistrations(crn) } returns listOf() // anything
      every { communityApiClient.getConvictions(crn) } returns listOf() // anything

      every { protectLevelCalculator.calculateProtectLevel(crn, any(), any(), any(), any()) } returns protectLevelResult
      every { changeLevelCalculator.calculateChangeLevel(crn, any(), any(), any(), any()) } returns changeLevelResult

      every { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) } returns null

      val slot = slot<TierCalculationEntity>()
      every { tierCalculationRepository.save(capture(slot)) } answers { slot.captured }

      val result = service.calculateTierForCrn(crn)

      assertThat(result.tierDto.protectLevel).isEqualTo(slot.captured.data.protect.tier)
      assertThat(result.tierDto.protectPoints).isEqualTo(slot.captured.data.protect.points)
      assertThat(result.tierDto.changeLevel).isEqualTo(slot.captured.data.change.tier)
      assertThat(result.tierDto.changePoints).isEqualTo(slot.captured.data.change.points)

      assertThat(result.isUpdated).isTrue

      verify { assessmentApiService.getRecentAssessment(crn) }
      verify { communityApiClient.getDeliusAssessments(crn) }
      verify { communityApiClient.getRegistrations(crn) }
      verify { communityApiClient.getConvictions(crn) }
      verify { protectLevelCalculator.calculateProtectLevel(crn, any(), any(), any(), any()) }
      verify { changeLevelCalculator.calculateChangeLevel(crn, any(), any(), any(), any()) }
      verify { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) }
      verify { tierCalculationRepository.save(capture(slot)) }
    }
  }
}
