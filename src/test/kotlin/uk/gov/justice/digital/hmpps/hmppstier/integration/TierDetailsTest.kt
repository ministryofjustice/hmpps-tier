package uk.gov.justice.digital.hmpps.hmppstier.integration

import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.tierToDeliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Registration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.TierDetails
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppstier.test.TestData
import java.util.*

class TierDetailsTest : IntegrationTestBase() {

    @Test
    fun `tier counts endpoint includes newly calculated case`() {
        val beforeCount = tierCounts().sumOf { it.count }
        val crn = TestData.crn()
        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(
                convictions = listOf(Conviction(sentenceCode = "SC")),
                registrations = listOf(Registration("M2")),
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 4234568890)

        calculateTierForDomainEvent(crn)
        expectLatestTierCalculation("A1")

        val afterCount = tierCounts().sumOf { it.count }
        assertThat(afterCount).isEqualTo(beforeCount + 1)
    }

    @Test
    fun `tier details endpoint returns latest calculation details`() {
        val crn = "X${UUID.randomUUID().toString().replace("-", "").take(6)}"
        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(
                convictions = listOf(Conviction(sentenceCode = "SC")),
                registrations = listOf(Registration("M2")),
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 4234568890)

        calculateTierForDomainEvent(crn)
        expectLatestTierCalculation("A1")

        mockMvc.perform(
            get("/crn/$crn/tier/details").headers(authHeaders()).contentType("application/json"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("tierScore", equalTo("A1")))
            .andExpect(jsonPath("calculationId").exists())
            .andExpect(jsonPath("calculationDate").exists())
            .andExpect(jsonPath("data").exists())
    }

    private fun tierCounts(): List<TierCountResponse> {
        val response = mockMvc.perform(
            get("/tier-counts").headers(authHeaders()).contentType("application/json"),
        )
            .andExpect(status().isOk)
            .andReturn()

        return objectMapper.readValue(response.response.contentAsString)
    }

    private data class TierCountResponse(
        val protectLevel: String,
        val changeLevel: Int,
        val count: Int,
    )
}
