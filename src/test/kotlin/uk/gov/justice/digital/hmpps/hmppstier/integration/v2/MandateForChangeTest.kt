package uk.gov.justice.digital.hmpps.hmppstier.integration.v2

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusConviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusRegistration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusRequirement
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.deliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppstier.test.TestData
import java.time.LocalDate

class MandateForChangeTest : IntegrationTestBase() {

    @Test
    fun `calculate change for concurrent custodial and non-custodial sentence`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            deliusResponse(
                convictions = listOf(
                    deliusConviction(),
                    deliusConviction(
                        sentenceCode = "SP",
                        requirements = listOf(deliusRequirement("X", restrictive = true))
                    )
                ),
                registrations = listOf(deliusRegistration(level = "M2")),
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 4234567892)
        calculateTierForDomainEvent(crn)
        expectTierChangedById("A1")
    }

    @Test
    fun `do not calculate change for terminated custodial sentence`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            deliusResponse(
                convictions = listOf(deliusConviction(terminationDate = LocalDate.now().minusDays(1))),
                registrations = listOf(deliusRegistration(level = "M2")),
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 4234567895)
        calculateTierForDomainEvent(crn)
        expectTierChangedById("A0")
    }

    @Test
    fun `calculate change for terminated non-custodial sentence and current non-custodial sentence with non-restrictive requirements`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            deliusResponse(
                convictions = listOf(
                    deliusConviction(
                        sentenceCode = "SP",
                        requirements = listOf(deliusRequirement("F", restrictive = false))
                    ),
                    deliusConviction(sentenceCode = "SP"),
                ),
                registrations = listOf(deliusRegistration(level = "M2")),
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 4234567896)
        calculateTierForDomainEvent(crn)
        expectTierChangedById("A1")
    }

    @Test
    fun `do not calculate change for terminated non-custodial sentence with non-restrictive requirements`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            deliusResponse(
                convictions = listOf(
                    deliusConviction(
                        terminationDate = LocalDate.now().minusDays(1),
                        requirements = listOf(deliusRequirement("F", restrictive = false))
                    ),
                ),
                registrations = listOf(deliusRegistration(level = "M2")),
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 4234567898)
        calculateTierForDomainEvent(crn)
        expectTierChangedById("A0")
    }

    @Test
    fun `do not calculate change when only restrictive requirements are present on a non-custodial sentence`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            deliusResponse(
                convictions = listOf(
                    deliusConviction(
                        sentenceCode = "SP",
                        requirements = listOf(deliusRequirement("X", restrictive = true))
                    ),
                ),
                registrations = listOf(deliusRegistration(level = "M2")),
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 4234567899)
        calculateTierForDomainEvent(crn)
        expectTierChangedById("A0")
    }

    @Test
    fun `calculate change with restrictive and non-restrictive requirements on a non-custodial sentence`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            deliusResponse(
                convictions = listOf(
                    deliusConviction(
                        sentenceCode = "SP",
                        requirements = listOf(
                            deliusRequirement("F", restrictive = false),
                            deliusRequirement("X", restrictive = true)
                        )
                    ),
                ),
                registrations = listOf(deliusRegistration(level = "M2")),
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 4134567890)
        calculateTierForDomainEvent(crn)
        expectTierChangedById("A1")
    }

    @Test
    fun `do not calculate change when no requirements are present on a non-custodial sentence`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            deliusResponse(
                convictions = listOf(deliusConviction(sentenceCode = "SP")),
                registrations = listOf(deliusRegistration(level = "M2")),
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 4334567890)
        calculateTierForDomainEvent(crn)
        expectTierChangedById("A0")
    }
}
