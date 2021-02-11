package uk.gov.justice.digital.hmpps.hmppstier.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.hmppstier.config.ObjectMapperConfiguration
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel.TWO
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel.B
import uk.gov.justice.digital.hmpps.hmppstier.dto.TierDto
import java.nio.file.Files
import java.nio.file.Paths

@ExtendWith(MockKExtension::class)
@DisplayName("Tier Calculation Event Listener tests")
class TierCalculationRequiredEventListenerTest {

  private val successUpdater: SuccessUpdater = mockk(relaxUnitFun = true)
  private val tierCalculationService: TierCalculationService = mockk(relaxUnitFun = true)
  private val objectMapper: ObjectMapper = ObjectMapperConfiguration().objectMapper()

  private val listener: TierCalculationRequiredEventListener =
    TierCalculationRequiredEventListener(objectMapper, tierCalculationService, successUpdater)

  private val protect = TierLevel(B, 0)
  private val change = TierLevel(TWO, 0)

  @BeforeEach
  fun resetAllMocks() {
    clearMocks(tierCalculationService)
  }

  @AfterEach
  fun confirmVerified() {
    io.mockk.confirmVerified(tierCalculationService)
  }

  @Test
  fun `Should call calculateTierForCrn`() {
    val validMessage: String =
      Files.readString(Paths.get("src/test/resources/fixtures/sqs/tier-calculation-event.json"))
    val crn = "X373878"

    val calculationResult = TierDto(
      protect.tier,
      protect.points,
      change.tier,
      change.points
    )
    every { tierCalculationService.calculateTierForCrn(crn) } returns
      calculationResult
    every { successUpdater.update(calculationResult) } returns Unit

    listener.listen(validMessage)

    verify { tierCalculationService.calculateTierForCrn(crn) }
  }

  @Test
  fun `should call community-api update tier on success`() {
    val validMessage: String =
      Files.readString(Paths.get("src/test/resources/fixtures/sqs/tier-calculation-event.json"))
    val crn = "X373878"

    val calculationResult = TierDto(
      protect.tier,
      protect.points,
      change.tier,
      change.points
    )
    every { tierCalculationService.calculateTierForCrn(crn) } returns
      calculationResult
    every { successUpdater.update(calculationResult) } returns Unit

    listener.listen(validMessage)

    verify { tierCalculationService.calculateTierForCrn(crn) }
    verify { successUpdater.update(calculationResult) }
  }
}
