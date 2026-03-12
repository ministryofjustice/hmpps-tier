package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.SectionAnswer
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.*
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.arnsApi.ArnsApiExtension.Companion.arnsApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusConviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusRegistration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.deliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppstier.test.TestData

class RecalculationTest : IntegrationTestBase() {

    @Test
    fun `providing crns recalculates only those crns`() {
        val crn = TestData.crn()
        val assessmentId = 5738261645L

        deliusApi.getFullDetails(
            crn,
            deliusResponse(
                convictions = listOf(deliusConviction(sentenceCode = "SC")),
                registrations = listOf(deliusRegistration(level = "M2")),
                latestReleaseDate = null,
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId)

        mockMvc.perform(
            post("/calculations?dryRun=false")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listOf(crn)))
                .headers(authHeaders()),
        ).andExpect(status().isOk)

        verify(telemetryClient, timeout(2000)).trackEvent(
            "TierChanged",
            mapOf(
                "crn" to crn,
                "protect" to "A",
                "change" to "1",
                "version" to "3",
                "recalculationSource" to "LimitedRecalculation",
            ),
            null,
        )
    }

    @Test
    fun `providing no crns recalculates all active crns from delius`() {
        val crns = (0..200).map { TestData.crn() }

        deliusApi.getCrns(crns)
        crns.forEach { crn ->
            deliusApi.getFullDetails(
                crn,
                deliusResponse(
                    convictions = listOf(deliusConviction(sentenceCode = "SC")),
                    registrations = listOf(deliusRegistration(level = "M2")),
                    latestReleaseDate = null,
                ),
            )

            val assessmentId = crn.drop(1).toLong()
            restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId)
        }

        mockMvc.perform(
            post("/calculations?dryRun=false")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(authHeaders()),
        ).andExpect(status().isOk)

        crns.forEach {
            verify(telemetryClient, timeout(2000)).trackEvent(
                "TierChanged",
                mapOf(
                    "crn" to it,
                    "protect" to "A",
                    "change" to "1",
                    "version" to "3",
                    "recalculationSource" to "FullRecalculation",
                ),
                null,
            )
        }
    }

    @Test
    fun `test recalculates with standard needs`() {
        val crn = TestData.crn()
        val assessmentId = 95464646L

        deliusApi.getFullDetails(
            crn,
            deliusResponse(
                convictions = listOf(deliusConviction(sentenceCode = "SC")),
                registrations = listOf(deliusRegistration(level = "M2")),
                latestReleaseDate = null,
            ),
        )
        arnsApi.getTierAssessmentDetails(
            crn, assessmentId, Need.entries.associateWith { NeedSeverity.STANDARD }, mapOf(
                IMPULSIVITY to SectionAnswer.Problem.Some,
                TEMPER_CONTROL to SectionAnswer.Problem.Some,
                PARENTING_RESPONSIBILITIES to SectionAnswer.YesNo.Yes
            )
        )

        mockMvc.perform(
            post("/calculations?dryRun=false")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listOf(crn)))
                .headers(authHeaders()),
        ).andExpect(status().isOk)

        verify(telemetryClient, timeout(2000)).trackEvent(
            "TierChanged",
            mapOf(
                "crn" to crn,
                "protect" to "A",
                "change" to "2",
                "version" to "3",
                "recalculationSource" to "LimitedRecalculation",
            ),
            null,
        )
    }

    @Test
    fun `test recalculates with severe needs`() {
        val crn = TestData.crn()
        val assessmentId = 95464646L

        deliusApi.getFullDetails(
            crn,
            deliusResponse(
                convictions = listOf(deliusConviction(sentenceCode = "SC")),
                registrations = listOf(deliusRegistration(level = "M2")),
                latestReleaseDate = null,
            ),
        )
        arnsApi.getTierAssessmentDetails(
            crn, assessmentId, Need.entries.associateWith { NeedSeverity.SEVERE }, mapOf(
                IMPULSIVITY to SectionAnswer.Problem.Significant,
                TEMPER_CONTROL to SectionAnswer.Problem.Significant,
                PARENTING_RESPONSIBILITIES to SectionAnswer.YesNo.Yes
            )
        )

        mockMvc.perform(
            post("/calculations?dryRun=false")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listOf(crn)))
                .headers(authHeaders()),
        ).andExpect(status().isOk)

        verify(telemetryClient, timeout(2000)).trackEvent(
            "TierChanged",
            mapOf(
                "crn" to crn,
                "protect" to "A",
                "change" to "3",
                "version" to "3",
                "recalculationSource" to "LimitedRecalculation",
            ),
            null,
        )
    }
}
