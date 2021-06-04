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
  private val communityApiService: CommunityApiService = mockk(relaxUnitFun = true)
  private val telemetryService: TelemetryService = mockk(relaxUnitFun = true)
  private val successUpdater: SuccessUpdater = mockk(relaxUnitFun = true)
  private val additionalFactorsForWomen: AdditionalFactorsForWomen = mockk(relaxUnitFun = true)
  private val mandateForChange: MandateForChange = mockk(relaxUnitFun = true)

  private val service = TierCalculationService(
    clock,
    tierCalculationRepository,
    changeLevelCalculator,
    assessmentApiService,
    communityApiService,
    successUpdater,
    telemetryService,
    additionalFactorsForWomen,
    mandateForChange
  )

  private val calculationId = UUID.randomUUID()
  private val crn = "Any Crn"
  private val protectLevelResult = TierLevel(ProtectLevel.B, 0, mapOf())
  private val changeLevelResult = TierLevel(ChangeLevel.TWO, 0, mapOf())
  private val validTierCalculationEntity = TierCalculationEntity(
    0,
    calculationId,
    crn,
    LocalDateTime.now(clock),
    TierCalculationResultEntity(protectLevelResult, changeLevelResult, "2")
  )

  @BeforeEach
  fun resetAllMocks() {
    clearMocks(tierCalculationRepository)
    clearMocks(changeLevelCalculator)
    clearMocks(protectLevelCalculator)
    clearMocks(assessmentApiService)
    clearMocks(telemetryService)
  }

  @AfterEach
  fun confirmVerified() {
    // Check we don't add any more calls without updating the tests
    confirmVerified(tierCalculationRepository)
    confirmVerified(changeLevelCalculator)
    confirmVerified(protectLevelCalculator)
    confirmVerified(assessmentApiService)
    confirmVerified(telemetryService)
  }

  @Nested
  @DisplayName("Get Tier By Crn tests")
  inner class GetTierByCrnTests {

    @Test
    fun `Should Call Collaborators Test - Existing found`() {
      every { tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn) } returns validTierCalculationEntity
      val result = service.getLatestTierByCrn(crn)

      assertThat(result?.tierScore).isEqualTo(
        validTierCalculationEntity.data.protect.tier.value.plus(
          validTierCalculationEntity.data.change.tier.value
        )
      )
      assertThat(result?.calculationId).isEqualTo(validTierCalculationEntity.uuid)

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
  @DisplayName("Get Tier By CalculationID tests")
  inner class GetTierByCalculationIdTests {

    @Test
    fun `Should Call Collaborators Test - Existing found`() {
      every { tierCalculationRepository.findByCrnAndUuid(crn, calculationId) } returns validTierCalculationEntity
      val result = service.getTierByCalculationId(crn, calculationId)

      assertThat(result?.tierScore).isEqualTo(
        validTierCalculationEntity.data.protect.tier.value.plus(
          validTierCalculationEntity.data.change.tier.value
        )
      )
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
}
