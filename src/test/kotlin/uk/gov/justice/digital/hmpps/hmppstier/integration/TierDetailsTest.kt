package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.arnsApi.ArnsApiExtension.Companion.arnsApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusConviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusRegistration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.deliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.TierApiVersion.V3
import uk.gov.justice.digital.hmpps.hmppstier.test.TestData

class TierDetailsTest : IntegrationTestBase() {

    @Test
    fun `tier details v2 endpoint returns latest calculation details`() {
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            deliusResponse(
                convictions = listOf(deliusConviction(sentenceCode = "SC")),
                registrations = listOf(deliusRegistration(level = "M2")),
                latestReleaseDate = null,
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 4234568890)

        calculateTierForDomainEvent(crn)
        expectLatestTierCalculation("A1")

        mockMvc.perform(get("/v2/crn/$crn/tier/details").headers(setAuthorisation()).contentType("application/json"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("tierScore", equalTo("A1")))
            .andExpect(jsonPath("calculationId").exists())
            .andExpect(jsonPath("calculationDate").exists())
            .andExpect(jsonPath("data").exists())
    }

    @Test
    fun `tier details v3 endpoint returns latest calculation details and updates counts`() {
        val beforeCount = tierCounts().values.sum()
        val crn = TestData.crn()
        deliusApi.getFullDetails(
            crn,
            deliusResponse(
                registrations = listOf(
                    deliusRegistration(category = "M2", level = "M2"),
                    deliusRegistration(typeCode = Rosh.VERY_HIGH.registerCode)
                ),
            ),
        )
        arnsApi.getRiskPredictors(crn, csrp = 5.5, arp = 10.1)

        calculateTierForDomainEvent(crn)
        expectLatestTierCalculation("A", V3)

        mockMvc.perform(get("/v3/crn/$crn/tier/details").headers(setAuthorisation()).contentType("application/json"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("tierScore", equalTo("A")))
            .andExpect(jsonPath("calculationId").exists())
            .andExpect(jsonPath("calculationDate").exists())
            .andExpect(jsonPath("data").exists())

        val afterCount = tierCounts().values.sum()
        assertThat(afterCount).isEqualTo(beforeCount + 1)
    }

    private fun tierCounts(): Map<String, Long> {
        val response = mockMvc
            .perform(get("/v3/tier-counts").headers(setAuthorisation()).contentType("application/json"))
            .andExpect(status().isOk)
            .andReturn()

        return objectMapper.readValue(response.response.contentAsString)
    }
}
