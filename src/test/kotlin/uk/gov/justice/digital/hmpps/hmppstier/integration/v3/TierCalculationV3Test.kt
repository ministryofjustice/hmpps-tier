package uk.gov.justice.digital.hmpps.hmppstier.integration.v3

import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusRegistration
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.arnsApi.ArnsApiExtension.Companion.arnsApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusConviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusRegistration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.deliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.TierApiVersion
import uk.gov.justice.digital.hmpps.hmppstier.test.TestData

class TierCalculationV3Test : IntegrationTestBase() {

    @Test
    fun `returns G when ARP score is missing from risk predictors`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(crn, deliusDetails())
        arnsApi.getRiskPredictors(crn)

        calculateTierForDomainEvent(crn)
        expectTierChangedById("G", TierApiVersion.V3)
    }

    @Test
    fun `maps ARP and CSRP risk predictors to expected v3 tier`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(crn, deliusDetails())
        arnsApi.getRiskPredictors(crn, arp = 75.0, csrp = 1.0)

        calculateTierForDomainEvent(crn)
        expectTierChangedById("C", TierApiVersion.V3)
    }

    @Test
    fun `falls back to rescored assessment predictors when ARNS has no OGRS4 result`() {
        val crn = "X765432" // Matches CRN in V7_1__add_test_data_rescored_assessments.sql
        deliusApi.getFullDetails(crn, deliusDetails())
        arnsApi.getNotFoundRiskPredictors(crn)

        calculateTierForDomainEvent(crn)
        expectTierChangedById("C", TierApiVersion.V3)

        verify(rescoredAssessmentService).getByCrn(eq(crn))
    }

    @Test
    fun `applies MAPPA and ROSH rules after predictor mapping`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            deliusDetails(
                deliusRegistration(level = "M2", category = "M2"),
                deliusRegistration(typeCode = Rosh.VERY_HIGH.registerCode),
            ),
        )
        arnsApi.getRiskPredictors(crn)

        calculateTierForDomainEvent(crn)
        expectTierChangedById("A", TierApiVersion.V3)
    }

    @Test
    fun `does write back when top level tier is unchanged`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(crn, deliusDetails())
        arnsApi.getRiskPredictors(crn, arp = 75.0, csrp = 1.0)

        calculateTierForDomainEvent(crn)
        expectTierChangedById("C", TierApiVersion.V3)

        deliusApi.getFullDetails(crn, deliusDetails())
        arnsApi.getRiskPredictors(crn, arp = 75.0, csrp = 1.0)

        calculateTierForDomainEvent(crn)
        expectTierChangedById("C", TierApiVersion.V3)
    }

    @Test
    fun `returns latest v3 tier calculation`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(crn, deliusDetails())
        arnsApi.getRiskPredictors(crn)

        calculateTierForDomainEvent(crn)
        expectLatestTierCalculation("G", TierApiVersion.V3)

        deliusApi.getFullDetails(
            crn,
            deliusDetails(deliusRegistration(typeCode = DeliusRegistration.CHILD_PROTECTION)),
        )
        arnsApi.getRiskPredictors(crn)

        calculateTierForRecallDomainEvent(crn)
        expectLatestTierCalculation("F", TierApiVersion.V3)

        tierHistory(crn, TierApiVersion.V3)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()", equalTo(2)))
            .andExpect(jsonPath("$.*.tierScore", equalTo(listOf("F", "G"))))
            .andExpect(
                jsonPath(
                    "$.*.changeReason",
                    equalTo(listOf("A recall to custody process was started", "A breach was concluded"))
                )
            )
    }

    @Test
    fun `404 from latest tier calculation if there is no calculation`() {
        expectLatestTierCalculationNotFound("XNOCALC", TierApiVersion.V3)
    }

    @Test
    fun `404 from specified tier calculation`() {
        expectTierCalculationNotFound(
            crn = "XNOCALC",
            id = "5118f557-211e-4457-b75b-6df1f996d308",
            version = TierApiVersion.V3,
        )
    }

    @Test
    fun `400 from named tier calculation if calculationId is not valid`() {
        expectTierCalculationBadRequest(
            crn = "XNOCALC",
            id = "made-up-calculation-id",
            version = TierApiVersion.V3,
        )
    }

    private fun deliusDetails(vararg registrations: DeliusRegistration) = deliusResponse(
        convictions = listOf(deliusConviction(sentenceCode = "SC")),
        registrations = registrations.toList(),
        latestReleaseDate = null,
    )
}
