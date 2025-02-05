package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.hmppstier.dto.TierDto
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.arnsApi.ArnsApiExtension.Companion.arnsApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.tierToDeliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Registration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.TierDetails
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import java.util.UUID

class NoAssessmentFoundTest : IntegrationTestBase() {

    @Test
    fun `changeLevel should be 2 if assessment returns 404`() {
        val crn = "X273878"
        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(
                convictions = listOf(Conviction()),
                registrations = listOf(
                    Registration("M2"),
                ),
            ),
        )
        arnsApi.getNotFoundAssessment(crn)

        calculateTierForDomainEvent(crn)
        expectTierChangedById("A2")
    }

    @Test
    fun `can deserialise historic records`() {
        val crn1 = "F987564"
        val tier1 = getTierCalculation(crn1)
        assertThat(tier1.tierScore).isEqualTo("B3")
        val byId1 = getTierCalculationById(crn1, tier1.calculationId)
        assertThat(byId1.tierScore).isEqualTo("B3")

        val crn2 = "F987546"
        val tier2 = getTierCalculation(crn2)
        assertThat(tier2.tierScore).isEqualTo("B2")
        val byId2 = getTierCalculationById(crn2, tier2.calculationId)
        assertThat(byId2.tierScore).isEqualTo("B2")
    }

    private fun getTierCalculation(crn: String): TierDto {
        val response = latestTierCalculationResult(crn)
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        return objectMapper.readValue(response, TierDto::class.java)
    }

    private fun getTierCalculationById(crn: String, id: UUID): TierDto {
        val response = tierCalculationResult(crn, id.toString())
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        return objectMapper.readValue(response, TierDto::class.java)
    }
}
