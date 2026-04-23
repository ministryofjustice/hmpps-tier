package uk.gov.justice.digital.hmpps.hmppstier.messaging.consumer

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import uk.gov.justice.digital.hmpps.hmppstier.domain.RecalculationSource.EventSource.DomainEventRecalculation
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService
import uk.gov.justice.digital.hmpps.hmppstier.test.TestData

@ExtendWith(MockitoExtension::class)
class DomainEventListenerTest {

    @Mock
    lateinit var calculator: TierCalculationService

    private lateinit var listener: DomainEventListener
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    @BeforeEach
    fun setUp() {
        listener = DomainEventListener(calculator, objectMapper)
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
