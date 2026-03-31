package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusConviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusRegistration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.deliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.TierApiVersion
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import uk.gov.justice.digital.hmpps.hmppstier.model.TierDto
import uk.gov.justice.digital.hmpps.hmppstier.test.TestData
import java.time.LocalDateTime

class TierControllerVersioningTest : IntegrationTestBase() {

    @Test
    fun `legacy endpoints are duplicated by v2 endpoints`() {
        val crn = TestData.crn()
        val historicCalculation = saveHistoricV2Calculation(crn)
        val uuid = historicCalculation.uuid.toString()

        val legacyLatest = getTierDto(latestTierCalculationResult(crn, TierApiVersion.LEGACY))
        val v2Latest = getTierDto(latestTierCalculationResult(crn, TierApiVersion.V2))
        assertThat(legacyLatest).isEqualTo(v2Latest)
        assertThat(v2Latest.tierScore).isEqualTo("B2")

        val legacyById = getTierDto(tierCalculationResult(crn, uuid, TierApiVersion.LEGACY))
        val v2ById = getTierDto(tierCalculationResult(crn, uuid, TierApiVersion.V2))
        assertThat(legacyById).isEqualTo(v2ById)
    }

    @Test
    fun `v3 endpoints only return v3 calculations`() {
        val crn = TestData.crn()
        val historicCalculation = saveHistoricV2Calculation(crn)
        val uuid = historicCalculation.uuid.toString()

        tierCalculationResult(crn, uuid, TierApiVersion.V2)
            .andExpect(status().isOk)
            .andExpect(jsonPath("tierScore", equalTo("B2")))

        tierCalculationResult(crn, uuid, TierApiVersion.V3)
            .andExpect(status().isNotFound)

        // history includes both V2 and V3
        tierHistory(crn, TierApiVersion.V3)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()", equalTo(1)))
            .andExpect(jsonPath("$[0].tierScore", equalTo("B2")))

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

        val v2Latest = getTierDto(latestTierCalculationResult(crn, TierApiVersion.V2))
        val v3Latest = getTierDto(latestTierCalculationResult(crn, TierApiVersion.V3))

        assertThat(v2Latest.tierScore).isEqualTo("A1")
        assertThat(v3Latest.tierScore).isEqualTo("G")
        assertThat(v2Latest.calculationId).isEqualTo(v3Latest.calculationId)

        tierHistory(crn, TierApiVersion.V2)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()", equalTo(2)))
            .andExpect(jsonPath("$.*.tierScore", equalTo(listOf("A1", "B2"))))

        tierHistory(crn, TierApiVersion.V3)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()", equalTo(2)))
            .andExpect(jsonPath("$.*.tierScore", equalTo(listOf("G", "B2"))))

        tierCalculationResult(crn, uuid, TierApiVersion.V3)
            .andExpect(status().isNotFound)
    }

    private fun saveHistoricV2Calculation(crn: String): TierCalculationEntity =
        tierCalculationRepository.save(
            TierCalculationEntity(
                crn = crn,
                created = LocalDateTime.now().minusDays(10),
                data = TierCalculationResultEntity(
                    tier = null,
                    protect = TierLevel(ProtectLevel.B, 0, mapOf()),
                    change = TierLevel(ChangeLevel.TWO, 0, mapOf()),
                    calculationVersion = "2",
                ),
            ),
        )

    private fun getTierDto(result: ResultActions): TierDto {
        val response = result
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString
        return objectMapper.readValue(response, TierDto::class.java)
    }
}
