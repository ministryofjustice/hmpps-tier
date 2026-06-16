package uk.gov.justice.digital.hmpps.hmppstier.messaging.consumer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class DomainEventTest {

    @ParameterizedTest
    @MethodSource("sentenceChangeReasons")
    fun `sentence events have descriptive change reasons`(eventType: String, reason: String) {
        assertThat(event(eventType).changeReason()).isEqualTo(reason)
    }

    @Test
    fun `registration and requirement reasons omit type detail when event metadata is absent`() {
        assertThat(event("probation-case.registration.added", additionalInformation = null).changeReason())
            .isEqualTo("A registration was added")
        assertThat(event("probation-case.requirement.created", additionalInformation = null).changeReason())
            .isEqualTo("A requirement was added")
    }

    @Test
    fun `identifiers and optional crns are nullable when event metadata is absent`() {
        val event = DomainEvent(
            eventType = "unknown.event",
            description = "Original description",
            personReference = DomainEvent.PersonReference(listOf(DomainEvent.Identifier("NOMS", "A1234AA"))),
            additionalInformation = null,
        )

        assertThat(event.crn).isNull()
        assertThat(event.sourceCrn).isNull()
        assertThat(event.targetCrn).isNull()
        assertThat(event.unmergedCrn).isNull()
        assertThat(event.reactivatedCrn).isNull()
        assertThat(event.recalculationSource).isNull()
        assertThat(event.changeReason()).isEqualTo("Original description")
    }

    private fun event(
        eventType: String,
        additionalInformation: Map<String, Any?>? = emptyMap(),
    ) = DomainEvent(
        eventType = eventType,
        description = "Original description",
        personReference = DomainEvent.PersonReference(listOf(DomainEvent.Identifier("CRN", "A000001"))),
        additionalInformation = additionalInformation,
    )

    companion object {
        @JvmStatic
        fun sentenceChangeReasons() = listOf(
            Arguments.arguments("probation-case.sentence.created", "A sentence was added"),
            Arguments.arguments("probation-case.sentence.amended", "A sentence was amended"),
            Arguments.arguments("probation-case.sentence.terminated", "A sentence was terminated"),
            Arguments.arguments("probation-case.sentence.unterminated", "A sentence was un-terminated"),
            Arguments.arguments("probation-case.sentence.deleted", "A sentence was removed"),
            Arguments.arguments("probation-case.sentence.moved", "A sentence was moved"),
        )
    }
}
