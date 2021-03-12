package uk.gov.justice.digital.hmpps.hmppstier.service

import com.microsoft.applicationinsights.TelemetryClient
import io.mockk.clearMocks
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.dto.CalculationResultDto
import uk.gov.justice.digital.hmpps.hmppstier.dto.TierDto
import java.util.UUID

@ExtendWith(MockKExtension::class)
@DisplayName("Telemetry Service tests")
internal class TelemetryServiceTest {

  private val client: TelemetryClient = mockk(relaxUnitFun = true)

  private val service = TelemetryService(client)

  private val crn = "ABC123"
  private val tierDto = TierDto(
    ProtectLevel.A,
    17,
    ChangeLevel.ONE,
    5,
    UUID.randomUUID()
  )

  private fun resultDto(isUpdated: Boolean): CalculationResultDto {
    return CalculationResultDto(
      tierDto, isUpdated
    )
  }

  @BeforeEach
  fun resetAllMocks() {
    clearMocks(client)
  }

  @AfterEach
  fun confirmVerified() {
    // Check we don't add any more calls without updating the tests
    io.mockk.confirmVerified(client)
  }

  @Test
  fun `Should emit TierChanged event when tier HAS changed`() {

    service.trackTierCalculated(crn, resultDto(true))

    verify {
      client.trackEvent(
        TelemetryEventType.TIER_CHANGED.eventName,
        mapOf(
          "crn" to crn,
          "protect" to tierDto.protectLevel.value,
          "change" to tierDto.changeLevel.value.toString()
        ),
        null
      )
    }
  }

  @Test
  fun `Should emit TierUnchanged event when tier HAS NOT changed`() {

    service.trackTierCalculated(crn, resultDto(false))

    verify {
      client.trackEvent(
        TelemetryEventType.TIER_UNCHANGED.eventName,
        mapOf(
          "crn" to crn,
          "protect" to tierDto.protectLevel.value,
          "change" to tierDto.changeLevel.value.toString()
        ),
        null
      )
    }
  }
}
