package uk.gov.justice.digital.hmpps.hmppstier.controller

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.tierToDeliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Registration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.TierDetails
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppstier.service.RecalculationSource
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService

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
    fun `no tier performed if domain event is not one the service is interested in`() {
        val crn = "N671982"
        sendDomainEvent(DomainEventsMessage("unknown.event.type", PersonReference(listOf(Identifiers("CRN", crn)))))
        runBlocking {
            verify(tierCalculationService, never()).calculateTierForCrn(
                crn,
                RecalculationSource.DomainEventRecalculation
            )
        }
    }
}
