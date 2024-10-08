package uk.gov.justice.digital.hmpps.hmppstier.controller

import org.junit.jupiter.api.Test
import org.mockito.kotlin.never
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.tierToDeliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Registration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.TierDetails
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppstier.service.RecalculationSource

class DomainEventsListenerTest : IntegrationTestBase() {
    @Test
    fun `can calculate tier on domain event`() {
        val crn = "X432777"
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
        val crn = "X432777"
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
        val target = "M987654"
        val source = "D987654"
        sendDomainEvent(
            DomainEventsMessage(
                eventType,
                PersonReference(listOf(Identifiers("CRN", target))),
                mapOf("sourceCRN" to source),
            )
        )
        verify(tierCalculationService, timeout(5000)).calculateTierForCrn(
            target,
            RecalculationSource.EventSource.DomainEventRecalculation(eventType),
            true
        )
        verify(tierCalculationService, timeout(5000)).deleteCalculationsForCrn(source, eventType)
    }

    @Test
    fun `tier details of unmerged offenders are recalculated appropriately`() {
        val eventType = "probation-case.unmerge.completed"
        val target = "M987655"
        val source = "D987655"
        sendDomainEvent(
            DomainEventsMessage(
                eventType,
                PersonReference(listOf(Identifiers("CRN", target))),
                mapOf("unmergedCRN" to target, "reactivatedCRN" to source),
            )
        )
        verify(tierCalculationService, timeout(5000)).calculateTierForCrn(
            target,
            RecalculationSource.EventSource.DomainEventRecalculation(eventType),
            true
        )
        verify(tierCalculationService, timeout(5000)).calculateTierForCrn(
            source,
            RecalculationSource.EventSource.DomainEventRecalculation(eventType),
            true
        )
    }

    @Test
    fun `tier details of gdpr deleted crn are deleted`() {
        val eventType = "probation-case.deleted.gdpr"
        val crn = "D765432"
        sendDomainEvent(
            DomainEventsMessage(
                eventType,
                PersonReference(listOf(Identifiers("CRN", crn)))
            )
        )
        verify(tierCalculationService, timeout(5000)).deleteCalculationsForCrn(crn, eventType)
        verify(tierCalculationService, never()).calculateTierForCrn(
            crn,
            RecalculationSource.EventSource.DomainEventRecalculation(eventType),
            true
        )
    }
}
