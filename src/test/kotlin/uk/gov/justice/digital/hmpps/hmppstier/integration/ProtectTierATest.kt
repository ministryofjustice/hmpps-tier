package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh.VERY_HIGH
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.tierToDeliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Registration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.TierDetails
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase

class ProtectTierATest : IntegrationTestBase() {

    @Test
    fun `Tier is A with Mappa M2`() {
        val crn = "X333477"
        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(
                convictions = listOf(Conviction()),
                registrations = listOf(
                    Registration("M2"),
                ),
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 5234567891)
        calculateTierFor(crn)
        expectTierChangedById("A1")
    }

    @Test
    fun `Tier is A with Mappa M3`() {
        val crn = "X333478"
        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(
                convictions = listOf(Conviction()),
                registrations = listOf(
                    Registration("M3"),
                ),
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 5234567892)
        calculateTierFor(crn)
        expectTierChangedById("A1")
    }

    @Test
    fun `Tier is A with ROSH VERY HIGH`() {
        val crn = "X333479"
        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(
                convictions = listOf(Conviction()),
                registrations = listOf(
                    Registration(typeCode = VERY_HIGH.registerCode),
                ),
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 5234567893)
        calculateTierFor(crn)
        expectTierChangedById("A1")
    }

    @Test
    fun `Tier is B with low Mappa but 31 points`() {
        val crn = "X333480"
        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(
                convictions = listOf(Conviction()),
                registrations = listOf(
                    Registration(registerLevel = "M1"),
                    Registration(typeCode = Rosh.HIGH.registerCode),
                    Registration(typeCode = "RCCO"),
                    Registration(typeCode = "RCPR"),
                    Registration(typeCode = "RCHD"),
                ),
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 5234567894)
        calculateTierFor(crn)
        expectTierChangedById("B1")
    }
}
