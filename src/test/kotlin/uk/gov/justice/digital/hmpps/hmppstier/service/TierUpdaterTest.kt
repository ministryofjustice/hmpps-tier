package uk.gov.justice.digital.hmpps.hmppstier.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock

@ExtendWith(MockitoExtension::class)
internal class TierUpdaterTest {
    @Mock
    internal lateinit var clock: Clock

    @Mock
    internal lateinit var assessmentApiService: AssessmentApiService

    @Mock
    internal lateinit var tierToDeliusApiService: TierToDeliusApiService

    @Mock
    internal lateinit var successUpdater: SuccessUpdater

    @Mock
    internal lateinit var telemetryService: TelemetryService

    @Mock
    internal lateinit var tierUpdater: TierUpdater

    @Mock
    internal lateinit var tierReader: TierReader

    @InjectMocks
    internal lateinit var tierCalculationService: TierCalculationService

    @Test
    fun `failure to remove tier logs to app insights`() {
        val crn = "D123456"
        val reason = "Events Terminated"
        val message = "Some issue with db"
        whenever(tierUpdater.removeTierCalculationsFor(crn)).thenThrow(RuntimeException(message))
        assertThrows<RuntimeException> { tierCalculationService.deleteCalculationsForCrn(crn, reason) }
        verify(telemetryService).trackEvent(
            TelemetryEventType.TIER_CALCULATION_REMOVAL_FAILED,
            mapOf("crn" to crn, "reasonToDelete" to reason, "failureReason" to message)
        )
    }
}