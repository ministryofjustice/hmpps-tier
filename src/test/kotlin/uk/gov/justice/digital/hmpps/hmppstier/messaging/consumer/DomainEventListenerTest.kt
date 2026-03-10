package uk.gov.justice.digital.hmpps.hmppstier.messaging.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
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

    private fun sqsMessage(
        sqsEventType: String,
        domainEventType: String,
        crn: String = TestData.crn(),
    ): String {
        val domainEvent = DomainEvent(
            eventType = domainEventType,
            description = "description",
            personReference = DomainEvent.PersonReference(listOf(DomainEvent.Identifier("CRN", crn))),
        )

        return objectMapper.writeValueAsString(
            mapOf(
                "Message" to objectMapper.writeValueAsString(domainEvent),
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
}
