package uk.gov.justice.digital.hmpps.hmppstier.integration.v2

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusRegistration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.deliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppstier.test.TestData

class NoSentenceTest : IntegrationTestBase() {

    @Test
    fun `Tier is calculated with change level zero when no sentence is found`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            deliusResponse(
                registrations = listOf(deliusRegistration(level = "M2")),
                latestReleaseDate = null,
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 5234567890)
        calculateTierForDomainEvent(crn)
        expectTierChangedById("A0")
    }
}
