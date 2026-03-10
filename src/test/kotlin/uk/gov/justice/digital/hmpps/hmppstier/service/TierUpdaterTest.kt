package uk.gov.justice.digital.hmpps.hmppstier.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppstier.domain.TelemetryEventType
import uk.gov.justice.digital.hmpps.hmppstier.messaging.publisher.DomainEventPublisher
import uk.gov.justice.digital.hmpps.hmppstier.service.api.AssessmentApiService
import uk.gov.justice.digital.hmpps.hmppstier.service.api.DeliusApiService
import uk.gov.justice.digital.hmpps.hmppstier.test.TestData
import java.time.Clock

@ExtendWith(MockitoExtension::class)
internal class TierUpdaterTest {
    @Mock
    internal lateinit var clock: Clock

    @Mock
    internal lateinit var assessmentApiService: AssessmentApiService

    @Mock
    internal lateinit var deliusApiService: DeliusApiService

    @Mock
    internal lateinit var domainEventPublisher: DomainEventPublisher

    @Mock
    internal lateinit var telemetryService: TelemetryService

    @Mock
    internal lateinit var tierUpdater: TierUpdater

    @Test
    fun `failure to remove tier logs to app insights`() {
        val tierCalculationService = TierCalculationService(
            clock,
            assessmentApiService,
            deliusApiService,
            domainEventPublisher,
            telemetryService,
            tierUpdater,
        )
        val crn = TestData.crn()
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