package uk.gov.justice.digital.hmpps.hmppstier.messaging.consumer

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import uk.gov.justice.digital.hmpps.hmppstier.domain.RecalculationSource.EventSource.DomainEventRecalculation
import uk.gov.justice.digital.hmpps.hmppstier.service.TelemetryService
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService
import uk.gov.justice.digital.hmpps.hmppstier.test.TestData
import java.util.concurrent.CompletionException

@ExtendWith(MockitoExtension::class)
class DomainEventListenerTest {

    @Mock
    lateinit var calculator: TierCalculationService

    @Mock
    lateinit var telemetryService: TelemetryService

    private lateinit var listener: DomainEventListener
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    @BeforeEach
    fun setUp() {
        listener = DomainEventListener(calculator, objectMapper, telemetryService)
    }

    @Test
    fun `risk assessment score events are ignored when wrapped event is not assessment summary`() {
        val message = sqsMessage(
            sqsEventType = "risk-assessment.scores.determined",
            domainEventType = "enforcement.breach.raised",
        )

        listener.listen(message)

        verifyNoInteractions(calculator)
    }

    @Test
    fun `assessment summary events are processed when wrapped by risk assessment score event`() {
        val crn = TestData.crn()
        val message = sqsMessage(
            sqsEventType = "risk-assessment.scores.determined",
            domainEventType = "assessment.summary.produced",
            crn = crn,
        )

        listener.listen(message)

        verify(calculator).calculateTierForCrn(eq(crn), any())
    }

    @Test
    fun `CONVICTION_CHANGED offender events are converted and processed as domain events`() {
        val crn = TestData.crn()
        val message = sqsMessage(
            sqsEventType = "CONVICTION_CHANGED",
            crn = crn,
            message = OffenderEvent(crn)
        )

        listener.listen(message)

        verify(calculator).calculateTierForCrn(
            eq(crn),
            eq(DomainEventRecalculation("conviction.changed", "The supervision status changed"))
        )
    }

    @Test
    fun `domain events without a CRN are ignored`() {
        val message = sqsMessage(
            sqsEventType = "probation-case.registration.added",
            message = DomainEvent(
                eventType = "probation-case.registration.added",
                description = "A registration was added",
                personReference = DomainEvent.PersonReference(emptyList()),
            ),
        )

        listener.listen(message)

        verifyNoInteractions(calculator)
    }

    @Test
    fun `processing failures are rethrown`() {
        val crn = TestData.crn()
        val failure = IllegalStateException("calculation failed")
        whenever(calculator.calculateTierForCrn(eq(crn), any())).thenThrow(failure)

        assertThatThrownBy {
            listener.listen(sqsMessage(sqsEventType = "probation-case.registration.added", crn = crn))
        }.isSameAs(failure)
    }

    @Test
    fun `known wrapper exceptions are unwrapped before reporting`() {
        val cause = IllegalStateException("actual failure")

        assertThat(DomainEventListener.unwrapKnownWrapperTypes(CompletionException(cause))).isSameAs(cause)
    }

    private fun sqsMessage(
        sqsEventType: String,
        domainEventType: String = sqsEventType,
        crn: String = TestData.crn(),
        message: Any = DomainEvent(
            eventType = domainEventType,
            description = "description",
            personReference = DomainEvent.PersonReference(listOf(DomainEvent.Identifier("CRN", crn))),
        )
    ) = objectMapper.writeValueAsString(
        mapOf(
            "Message" to objectMapper.writeValueAsString(message),
            "MessageAttributes" to mapOf(
                "attributes" to mapOf(
                    "eventType" to mapOf(
                        "Type" to "String",
                        "Value" to sqsEventType,
                    ),
                ),
            ),
        ),
    )
}
