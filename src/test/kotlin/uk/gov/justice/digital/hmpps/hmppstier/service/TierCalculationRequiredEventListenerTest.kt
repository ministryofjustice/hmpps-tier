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
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
@DisplayName("Tier Calculation Event Listener tests")
class TierCalculationRequiredEventListenerTest {

  private val tierCalculationService: TierCalculationService = mockk(relaxUnitFun = true)
  private val objectMapper: ObjectMapper = ObjectMapperConfiguration().objectMapper()

  private val listener: TierCalculationRequiredEventListener =
    TierCalculationRequiredEventListener(objectMapper, tierCalculationService)

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
    val validMessage: String = Files.readString(Paths.get("src/test/resources/fixtures/sqs/tier-calculation-event.json"))
    val crn: String = "X373878"
    val tierLetterResult = TierLevel(ProtectLevel.B, 0)
    val tierNumberResult = TierLevel(ChangeLevel.TWO, 0)
    every { tierCalculationService.calculateTierForCrn(crn) } returns
      TierCalculationEntity(
        1,
        crn,
        LocalDateTime.now(),
        TierCalculationResultEntity(tierLetterResult, tierNumberResult)
      )

    listener.listen(validMessage)

    verify { tierCalculationService.calculateTierForCrn(crn) }
  }
}
