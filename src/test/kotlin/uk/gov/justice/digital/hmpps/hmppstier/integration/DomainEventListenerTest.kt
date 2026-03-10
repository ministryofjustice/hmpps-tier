package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.never
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppstier.domain.RecalculationSource
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.tierToDeliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Registration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.TierDetails
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppstier.messaging.consumer.DomainEvent
import uk.gov.justice.digital.hmpps.hmppstier.test.TestData

class DomainEventListenerTest : IntegrationTestBase() {
    @Test
    fun `can calculate tier on domain event`() {
        val crn = TestData.crn()
        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(
                convictions = listOf(Conviction(sentenceCode = "SC")),
                registrations = listOf(
                    Registration("M2"),
                ),
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 4234568890)

        calculateTierForDomainEvent(crn)
        expectLatestTierCalculation("A1")
    }

    @Test
    fun `can calculate tier on recall domain event`() {
        val crn = TestData.crn()
        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(
                currentTier = "UD2",
                convictions = listOf(Conviction(sentenceCode = "SC")),
                registrations = listOf(
                    Registration("M2"),
                ),
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 4234568890)

        calculateTierForRecallDomainEvent(crn)
        expectLatestTierCalculation("A1")
    }

    @Test
    fun `tier details of merged offender are deleted and recalculated appropriately`() {
        val eventType = "probation-case.merge.completed"
        val target = TestData.crn()
        val source = TestData.crn()
        sendDomainEvent(
            DomainEvent(
                eventType,
                "A case has been merged",
                DomainEvent.PersonReference(listOf(DomainEvent.Identifier("CRN", target))),
                mapOf("sourceCRN" to source, "targetCRN" to target),
            )
        )
        verify(tierCalculationService, timeout(5000)).calculateTierForCrn(
            target,
            RecalculationSource.EventSource.DomainEventRecalculation(
                eventType,
                "The case was merged from $source into $target"
            )
        )
        verify(tierCalculationService, timeout(5000)).deleteCalculationsForCrn(source, eventType)
    }

    @Test
    fun `tier details of unmerged offenders are recalculated appropriately`() {
        val eventType = "probation-case.unmerge.completed"
        val target = "M987655"
        val source = "D987655"
        sendDomainEvent(
            DomainEvent(
                eventType,
                "A case has been unmerged",
                DomainEvent.PersonReference(listOf(DomainEvent.Identifier("CRN", target))),
                mapOf("unmergedCRN" to target, "reactivatedCRN" to source),
            )
        )
        verify(tierCalculationService, timeout(5000)).calculateTierForCrn(
            target,
            RecalculationSource.EventSource.DomainEventRecalculation(
                eventType,
                "The case was un-merged from M987655 and D987655"
            )
        )
        verify(tierCalculationService, timeout(5000)).calculateTierForCrn(
            source,
            RecalculationSource.EventSource.DomainEventRecalculation(
                eventType,
                "The case was un-merged from M987655 and D987655"
            )
        )
    }

    @Test
    fun `tier details of gdpr deleted crn are deleted`() {
        val eventType = "probation-case.deleted.gdpr"
        val crn = TestData.crn()
        sendDomainEvent(
            DomainEvent(
                eventType,
                "A case has been deleted",
                DomainEvent.PersonReference(listOf(DomainEvent.Identifier("CRN", crn)))
            )
        )
        verify(tierCalculationService, timeout(5000)).deleteCalculationsForCrn(crn, eventType)
        verify(tierCalculationService, never()).calculateTierForCrn(
            crn,
            RecalculationSource.EventSource.DomainEventRecalculation(eventType, "A case has been deleted")
        )
    }

    @ParameterizedTest
    @MethodSource("changeReasons")
    fun `tier change reasons are derived from domain events`(event: DomainEvent, reason: String) {
        Assertions.assertThat(event.changeReason()).isEqualTo(reason)
    }

    companion object {
        private val event = DomainEvent(
            eventType = "type",
            description = "description",
            personReference = DomainEvent.PersonReference(listOf(DomainEvent.Identifier("CRN", "A000001"))),
            additionalInformation = mapOf(
                "sourceCRN" to "A000001",
                "targetCRN" to "A000002",
                "unmergedCRN" to "A000003",
                "reactivatedCRN" to "A000004",
                "registerTypeDescription" to "High RoSH",
                "requirementMainType" to "Unpaid Work",
            )
        )

        @JvmStatic
        fun changeReasons() = listOf(
            Arguments.arguments(event.copy(eventType = "enforcement.breach.concluded"), "A breach was concluded"),
            Arguments.arguments(event.copy(eventType = "enforcement.breach.raised"), "A breach was raised"),
            Arguments.arguments(
                event.copy(eventType = "enforcement.recall.concluded"),
                "A recall to custody process was concluded"
            ),
            Arguments.arguments(
                event.copy(eventType = "enforcement.recall.raised"),
                "A recall to custody process was started"
            ),
            Arguments.arguments(event.copy(eventType = "probation-case.engagement.created"), "The case was created"),
            Arguments.arguments(
                event.copy(eventType = "probation-case.merge.completed"),
                "The case was merged from A000001 into A000002"
            ),
            Arguments.arguments(
                event.copy(eventType = "probation-case.unmerge.completed"),
                "The case was un-merged from A000003 and A000004"
            ),
            Arguments.arguments(
                event.copy(eventType = "probation-case.registration.added"),
                "A registration of type 'High RoSH' was added"
            ),
            Arguments.arguments(
                event.copy(eventType = "probation-case.registration.deleted"),
                "A registration of type 'High RoSH' was removed"
            ),
            Arguments.arguments(
                event.copy(eventType = "probation-case.registration.deregistered"),
                "A registration of type 'High RoSH' was removed"
            ),
            Arguments.arguments(
                event.copy(eventType = "probation-case.registration.updated"),
                "A registration of type 'High RoSH' was updated"
            ),
            Arguments.arguments(
                event.copy(eventType = "probation-case.requirement.created"),
                "A requirement of type 'Unpaid Work' was added"
            ),
            Arguments.arguments(
                event.copy(eventType = "probation-case.requirement.deleted"),
                "A requirement of type 'Unpaid Work' was removed"
            ),
            Arguments.arguments(
                event.copy(eventType = "probation-case.requirement.terminated"),
                "A requirement of type 'Unpaid Work' was terminated"
            ),
            Arguments.arguments(
                event.copy(eventType = "probation-case.requirement.unterminated"),
                "A requirement of type 'Unpaid Work' was un-terminated"
            ),
            Arguments.arguments(
                event.copy(eventType = "risk-assessment.scores.determined"),
                "An OASys assessment was produced"
            ),
        )
    }
}