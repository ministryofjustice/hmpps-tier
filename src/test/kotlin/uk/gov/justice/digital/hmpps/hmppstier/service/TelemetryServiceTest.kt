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
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
@DisplayName("Telemetry Service tests")
internal class TelemetryServiceTest {

  private val client: TelemetryClient = mockk(relaxUnitFun = true)

  private val service = TelemetryService(client)

  private val crn = "ABC123"
  private val tierCalculation = TierCalculationEntity(
    0,
    UUID.randomUUID(),
    crn,
    LocalDateTime.now(),
    TierCalculationResultEntity(
      TierLevel(
        ProtectLevel.A, 17, mapOf()
      ),
      TierLevel(
        ChangeLevel.ONE, 5, mapOf()
      ),
      77
    )
  )

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

    service.trackTierCalculated(crn, tierCalculation, true)

    verify {
      client.trackEvent(
        TelemetryEventType.TIER_CHANGED.eventName,
        mapOf(
          "crn" to crn,
          "protect" to tierCalculation.data.protect.tier.value,
          "change" to tierCalculation.data.change.tier.value.toString(),
          "version" to tierCalculation.data.calculationVersion.toString()
        ),
        null
      )
    }
  }

  @Test
  fun `Should emit TierUnchanged event when tier HAS NOT changed`() {

    service.trackTierCalculated(crn, tierCalculation, false)

    verify {
      client.trackEvent(
        TelemetryEventType.TIER_UNCHANGED.eventName,
        mapOf(
          "crn" to crn,
          "protect" to tierCalculation.data.protect.tier.value,
          "change" to tierCalculation.data.change.tier.value.toString(),
          "version" to tierCalculation.data.calculationVersion.toString()
        ),
        null
      )
    }
  }
}
