package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.hmppstier.client.SectionAnswer
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.*
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.arnsApi.ArnsApiExtension.Companion.arnsApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.tierToDeliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Registration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.TierDetails
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase

class TriggerRecalculationsTest : IntegrationTestBase() {

    @Test
    fun `providing crns recalculates only those crns`() {
        val crn = "T123456"
        val assessmentId = 5738261645L

        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(
                convictions = listOf(Conviction(sentenceCode = "SC")),
                registrations = listOf(
                    Registration("M2"),
                ),
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
                "crn" to "T123456",
                "protect" to "A",
                "change" to "1",
                "version" to "2",
                "recalculationSource" to "LimitedRecalculation",
            ),
            null,
        )
    }

    @Test
    fun `providing no crns recalculates all active crns from delius`() {
        val startNumber = 123456
        val crns = (0..200).map { "D${it + startNumber}" }

        tierToDeliusApi.getCrns(crns)
        crns.forEachIndexed { index, crn ->
            tierToDeliusApi.getFullDetails(
                crn,
                TierDetails(
                    convictions = listOf(Conviction(sentenceCode = "SC")),
                    registrations = listOf(
                        Registration("M2"),
                    ),
                ),
            )

            val assessmentId = (index + startNumber).toLong()
            restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId)
        }

        mockMvc.perform(
            post("/calculations?dryRun=false")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(authHeaders()),
        ).andExpect(status().isOk)

        crns.forEach {
            verify(telemetryClient, timeout(20000)).trackEvent(
                "TierChanged",
                mapOf(
                    "crn" to it,
                    "protect" to "A",
                    "change" to "1",
                    "version" to "2",
                    "recalculationSource" to "FullRecalculation",
                ),
                null,
            )
        }
    }

    @Test
    fun `test recalculates with standard needs`() {
        val crn = "S123456"
        val assessmentId = 95464646L

        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(
                convictions = listOf(Conviction(sentenceCode = "SC")),
                registrations = listOf(Registration("M2")),
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
                "crn" to "S123456",
                "protect" to "A",
                "change" to "2",
                "version" to "2",
                "recalculationSource" to "LimitedRecalculation",
            ),
            null,
        )
    }

    @Test
    fun `test recalculates with severe needs`() {
        val crn = "S123457"
        val assessmentId = 95464646L

        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(
                convictions = listOf(Conviction(sentenceCode = "SC")),
                registrations = listOf(Registration("M2")),
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
                "crn" to "S123457",
                "protect" to "A",
                "change" to "3",
                "version" to "2",
                "recalculationSource" to "LimitedRecalculation",
            ),
            null,
        )
    }
}
