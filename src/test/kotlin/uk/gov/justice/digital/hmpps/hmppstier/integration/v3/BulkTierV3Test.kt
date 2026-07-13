package uk.gov.justice.digital.hmpps.hmppstier.integration.v3

import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.arnsApi.ArnsApiExtension.Companion.arnsApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusConviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.deliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.TierApiVersion
import uk.gov.justice.digital.hmpps.hmppstier.test.TestData

class BulkTierV3Test : IntegrationTestBase() {

    @Test
    fun `returns tiers for crns with existing summaries`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            deliusResponse(
                convictions = listOf(deliusConviction(sentenceCode = "SC")),
                latestReleaseDate = null,
            ),
        )
        arnsApi.getRiskPredictors(crn)

        calculateTierForDomainEvent(crn)
        expectLatestTierCalculation("G", TierApiVersion.V3)

        mockMvc.perform(
            post("/v3/crns/tier")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listOf(crn)))
                .headers(setAuthorisation()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()", equalTo(1)))
            .andExpect(jsonPath("$.$crn.tierScore", equalTo("G")))
            .andExpect(jsonPath("$.$crn.calculationId").isNotEmpty)
            .andExpect(jsonPath("$.$crn.calculationDate").isNotEmpty)
    }

    @Test
    fun `returns tiers for multiple crns`() {
        val crn1 = TestData.crn()
        val crn2 = TestData.crn()

        listOf(crn1, crn2).forEach { crn ->
            deliusApi.getFullDetails(
                crn,
                deliusResponse(
                    convictions = listOf(deliusConviction(sentenceCode = "SC")),
                    latestReleaseDate = null,
                ),
            )
            arnsApi.getRiskPredictors(crn)
            calculateTierForDomainEvent(crn)
            expectLatestTierCalculation("G", TierApiVersion.V3)
        }

        mockMvc.perform(
            post("/v3/crns/tier")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listOf(crn1, crn2)))
                .headers(setAuthorisation()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()", equalTo(2)))
            .andExpect(jsonPath("$.$crn1.tierScore", equalTo("G")))
            .andExpect(jsonPath("$.$crn2.tierScore", equalTo("G")))
    }

    @Test
    fun `falls back to on-demand calculation when no summary or calculation exists`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            deliusResponse(
                convictions = listOf(deliusConviction(sentenceCode = "SC")),
                latestReleaseDate = null,
            ),
        )
        arnsApi.getRiskPredictors(crn)

        mockMvc.perform(
            post("/v3/crns/tier")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listOf(crn)))
                .headers(setAuthorisation()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()", equalTo(1)))
            .andExpect(jsonPath("$.$crn.tierScore", equalTo("G")))
    }

    @Test
    fun `returns null for crn where no tier can be calculated`() {
        val crn = TestData.crn()
        deliusApi.getNotFound(crn)

        mockMvc.perform(
            post("/v3/crns/tier")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listOf(crn)))
                .headers(setAuthorisation()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()", equalTo(1)))
            .andExpect(jsonPath("$.$crn").value(nullValue()))
    }

    @Test
    fun `returns 400 when more than 20 crns are requested`() {
        val crns = (1..21).map { TestData.crn() }

        mockMvc.perform(
            post("/v3/crns/tier")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(crns))
                .headers(setAuthorisation()),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `returns 200 with empty map when no crns provided`() {
        mockMvc.perform(
            post("/v3/crns/tier")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyList<String>()))
                .headers(setAuthorisation()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()", equalTo(0)))
    }

    @Test
    fun `returns 401 when not authenticated`() {
        mockMvc.perform(
            post("/v3/crns/tier")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listOf("X123456"))),
        )
            .andExpect(status().isUnauthorized)
    }
}
