package uk.gov.justice.digital.hmpps.hmppstier.integration.v2

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh.VERY_HIGH
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusConviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusRegistration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.deliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppstier.test.TestData

class ProtectTierATest : IntegrationTestBase() {

    @Test
    fun `Tier is A with Mappa M2`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            deliusResponse(
                convictions = listOf(deliusConviction()),
                registrations = listOf(deliusRegistration(level = "M2")),
                latestReleaseDate = null,
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 5234567891)
        calculateTierForDomainEvent(crn)
        expectTierChangedById("A1")
    }

    @Test
    fun `Tier is A with Mappa M3`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            deliusResponse(
                convictions = listOf(deliusConviction()),
                registrations = listOf(deliusRegistration(level = "M3")),
                latestReleaseDate = null,
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 5234567892)
        calculateTierForDomainEvent(crn)
        expectTierChangedById("A1")
    }

    @Test
    fun `Tier is A with ROSH VERY HIGH`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            deliusResponse(
                convictions = listOf(deliusConviction()),
                registrations = listOf(deliusRegistration(typeCode = VERY_HIGH.registerCode)),
                latestReleaseDate = null,
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 5234567893)
        calculateTierForDomainEvent(crn)
        expectTierChangedById("A1")
    }

    @Test
    fun `Tier is B with low Mappa but 31 points`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            deliusResponse(
                convictions = listOf(deliusConviction()),
                registrations = listOf(
                    deliusRegistration(level = "M1"),
                    deliusRegistration(typeCode = Rosh.HIGH.registerCode),
                    deliusRegistration(typeCode = "RCCO"),
                    deliusRegistration(typeCode = "RCPR"),
                    deliusRegistration(typeCode = "RCHD"),
                ),
                latestReleaseDate = null,
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 5234567894)
        calculateTierForDomainEvent(crn)
        expectTierChangedById("B1")
    }
}
