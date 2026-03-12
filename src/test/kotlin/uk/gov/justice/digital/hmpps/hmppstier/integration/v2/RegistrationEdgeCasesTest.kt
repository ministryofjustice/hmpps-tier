package uk.gov.justice.digital.hmpps.hmppstier.integration.v2

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusRegistration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.arnsApi.ArnsApiExtension
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.deliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppstier.test.TestData
import java.time.LocalDate

class RegistrationEdgeCasesTest : IntegrationTestBase() {

    @Test
    fun `calculate change and protect when no registrations are found`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            ResponseGenerator.deliusResponse(
                convictions = listOf(ResponseGenerator.deliusConviction()),
                latestReleaseDate = null,
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 7234567890)
        calculateTierForDomainEvent(crn)
        expectTierChangedById("B1")
    }

    @Test
    fun `calculate change and protect when registration level is missing`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            ResponseGenerator.deliusResponse(
                convictions = listOf(ResponseGenerator.deliusConviction()),
                registrations = listOf(ResponseGenerator.deliusRegistration(typeCode = "STRG")),
                latestReleaseDate = null,
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 6234567890)
        calculateTierForDomainEvent(crn)
        expectTierChangedById("B1")
    }

    @Test
    fun `uses latest registration - two mappa registrations present`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            ResponseGenerator.deliusResponse(
                ogrsScore = null,
                rsrScore = null,
                convictions = listOf(ResponseGenerator.deliusConviction()),
                registrations = listOf(
                    ResponseGenerator.deliusRegistration(level = "M1", date = LocalDate.of(2020, 2, 1)),
                    ResponseGenerator.deliusRegistration(level = "M2", date = LocalDate.of(2021, 2, 1)),
                ),
                latestReleaseDate = null,
            ),
        )
        ArnsApiExtension.Companion.arnsApi.getNotFoundAssessment(crn)
        calculateTierForDomainEvent(crn)
        expectTierChangedById("A2")
    }

    @Test
    fun `exclude historic registrations from tier calculation`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            ResponseGenerator.deliusResponse(
                ogrsScore = null,
                rsrScore = null,
                convictions = listOf(ResponseGenerator.deliusConviction()),
                registrations = listOf(
                    ResponseGenerator.deliusRegistration(level = "M2", typeCode = "HREG"),
                ),
                latestReleaseDate = null,
            ),
        )
        ArnsApiExtension.Companion.arnsApi.getNotFoundAssessment(crn)
        calculateTierForDomainEvent(crn)
        expectLatestTierCalculation("D2")
    }

    @Test
    fun `uses mappa registration when latest is non-mappa but has mappa registration level`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            ResponseGenerator.deliusResponse(
                convictions = listOf(ResponseGenerator.deliusConviction()),
                registrations = listOf(
                    ResponseGenerator.deliusRegistration(
                        level = "M2",
                        typeCode = "HREG",
                        date = LocalDate.of(2016, 6, 28)
                    ),
                    ResponseGenerator.deliusRegistration(level = "M0", date = LocalDate.of(2008, 10, 24)),
                ),
                latestReleaseDate = null,
            ),
        )
        ArnsApiExtension.Companion.arnsApi.getNotFoundAssessment(crn)
        calculateTierForDomainEvent(crn)
        expectTierChangedById("B2")
    }

    @Test
    fun `uses two thirds registration to add suffix to tier`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            ResponseGenerator.deliusResponse(
                convictions = listOf(ResponseGenerator.deliusConviction()),
                registrations = listOf(
                    ResponseGenerator.deliusRegistration(
                        level = "M2",
                        typeCode = "HREG",
                        date = LocalDate.of(2016, 6, 28)
                    ),
                    ResponseGenerator.deliusRegistration(
                        typeCode = DeliusRegistration.Companion.TWO_THIRDS_CODE,
                        date = LocalDate.of(2024, 3, 22)
                    ),
                ),
                latestReleaseDate = null,
            ),
        )
        ArnsApiExtension.Companion.arnsApi.getNotFoundAssessment(crn)
        calculateTierForDomainEvent(crn)
        expectTierChangedById("B2S")
    }
}