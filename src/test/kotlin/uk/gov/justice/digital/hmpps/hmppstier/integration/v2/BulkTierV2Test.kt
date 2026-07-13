package uk.gov.justice.digital.hmpps.hmppstier.integration.v2

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
import uk.gov.justice.digital.hmpps.hmppstier.test.TestData

class BulkTierV2Test : IntegrationTestBase() {

    @Test
    fun `returns tiers for crns with existing summaries`() {
        // F987546 and F987564 have pre-existing tier calculations from test data migration
        // Trigger a calculation to ensure summaries are created
        val crn = "F987546"
        deliusApi.getFullDetails(
            crn,
            deliusResponse(
                convictions = listOf(deliusConviction()),
                latestReleaseDate = null,
            ),
        )
        arnsApi.getNotFoundAssessment(crn)
        calculateTierForDomainEvent(crn)
        expectLatestTierCalculation("B2")

        mockMvc.perform(
            post("/v2/crns/tier")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listOf(crn)))
                .headers(setAuthorisation()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()", equalTo(1)))
            .andExpect(jsonPath("$.$crn.tierScore", equalTo("B2")))
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
                    convictions = listOf(deliusConviction()),
                    latestReleaseDate = null,
                ),
            )
            arnsApi.getNotFoundAssessment(crn)
            calculateTierForDomainEvent(crn)
            expectLatestTierCalculation("B2")
        }

        mockMvc.perform(
            post("/v2/crns/tier")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listOf(crn1, crn2)))
                .headers(setAuthorisation()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()", equalTo(2)))
            .andExpect(jsonPath("$.$crn1.tierScore", equalTo("B2")))
            .andExpect(jsonPath("$.$crn2.tierScore", equalTo("B2")))
    }

    @Test
    fun `falls back to on-demand calculation when no summary or calculation exists`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            deliusResponse(
                convictions = listOf(deliusConviction()),
                latestReleaseDate = null,
            ),
        )
        arnsApi.getNotFoundAssessment(crn)

        mockMvc.perform(
            post("/v2/crns/tier")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listOf(crn)))
                .headers(setAuthorisation()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()", equalTo(1)))
            .andExpect(jsonPath("$.$crn.tierScore", equalTo("B2")))
    }

    @Test
    fun `returns null for crn where no tier can be calculated`() {
        val crn = TestData.crn()
        deliusApi.getNotFound(crn)

        mockMvc.perform(
            post("/v2/crns/tier")
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
            post("/v2/crns/tier")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(crns))
                .headers(setAuthorisation()),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `returns 200 with empty map when no crns provided`() {
        mockMvc.perform(
            post("/v2/crns/tier")
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
            post("/v2/crns/tier")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listOf("X123456"))),
        )
            .andExpect(status().isUnauthorized)
    }
}
