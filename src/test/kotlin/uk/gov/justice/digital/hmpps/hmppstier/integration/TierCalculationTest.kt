package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.arnsApi.ArnsApiExtension.Companion.arnsApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.tierToDeliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Registration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.TierDetails
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase

class TierCalculationTest : IntegrationTestBase() {

    @Test
    fun `no NSis returned Female Offender`() {
        val crn = "X386786"
        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(
                gender = "Female",
                ogrsScore = null,
                rsrScore = null,
                convictions = listOf(
                    Conviction(),
                ),
            ),
        )
        arnsApi.getNotFoundAssessment(crn)

        calculateTierForDomainEvent(crn)
        expectTierChangedById("D2")
    }

    @Test
    fun `Does write back when tier is unchanged`() {
        val crn = "X432769"
        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(
                convictions = listOf(Conviction(sentenceCode = "SC")),
                registrations = listOf(
                    Registration("M2"),
                ),
            ),
        )
        arnsApi.getNotFoundAssessment(crn)

        calculateTierForDomainEvent(crn)
        expectTierChangedById("A2")

        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(
                convictions = listOf(Conviction(sentenceCode = "SC")),
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
    fun `Does write back when calculation result differs but tier is unchanged`() {
        val crn = "X432779"
        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(
                convictions = listOf(Conviction(sentenceCode = "SC")),
                registrations = listOf(
                    Registration("M2"),
                ),
            ),
        )
        arnsApi.getNotFoundAssessment(crn)

        calculateTierForDomainEvent(crn)
        expectTierChangedById("A2")

        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(
                ogrsScore = "0",
                convictions = listOf(Conviction(sentenceCode = "SC")),
                registrations = listOf(
                    Registration("M2"),
                ),
            ),
        )
        arnsApi.getTierAssessmentDetails(crn, 4234568899, Need.entries.associateWith { NeedSeverity.SEVERE })

        calculateTierForDomainEvent(crn)
        expectTierChangedById("A3")
    }

    @Test
    fun `writes back when change level is changed`() {
        val crn = "X432770"
        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(
                convictions = listOf(Conviction(sentenceCode = "SC")),
                registrations = listOf(
                    Registration("M2"),
                ),
            ),
        )
        arnsApi.getNotFoundAssessment(crn)

        calculateTierForDomainEvent(crn)
        expectTierChangedById("A2")

        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(
                convictions = listOf(Conviction(sentenceCode = "SC")),
                registrations = listOf(
                    Registration("M2"),
                ),
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, 4234568891)
        calculateTierForDomainEvent(crn)
        expectTierChangedById("A1")
    }

    @Test
    fun `writes back when protect level is changed`() {
        val crn = "X432771"
        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(
                convictions = listOf(Conviction(sentenceCode = "SC")),
                registrations = listOf(
                    Registration("M2"),
                ),
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 4234568890)

        calculateTierForDomainEvent(crn)
        expectTierChangedById("A1")

        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(
                convictions = listOf(Conviction(sentenceCode = "SC")),
                registrations = listOf(
                    Registration("M1"),
                ),
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 4234568890)

        calculateTierForDomainEvent(crn)
        expectTierChangedById("B1")
    }

    @Test
    fun `returns latest tier calculation`() {
        val crn = "X432777"
        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(
                convictions = listOf(Conviction(sentenceCode = "SC")),
                registrations = listOf(
                    Registration("M2"),
                ),
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 4234568890)

        calculateTierForDomainEvent(crn)
        expectLatestTierCalculation("A1")

        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(
                convictions = listOf(Conviction(sentenceCode = "SC")),
                registrations = listOf(
                    Registration("M1"),
                ),
            ),
        )
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 4234568890)

        calculateTierForRecallDomainEvent(crn)
        expectLatestTierCalculation("B1")

        tierHistory(crn)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()", equalTo(2)))
            .andExpect(
                jsonPath(
                    "$.*.changeReason",
                    equalTo(listOf("A recall to custody process was started", "A breach was concluded"))
                )
            )
    }

    @Test
    fun `404 from latest tier calculation if there is no calculation`() {
        val crn = "XNOCALC"
        expectLatestTierCalculationNotFound(crn)
    }

    @Test
    fun `404 from specified tier calculation`() {
        val crn = "XNOCALC"
        expectTierCalculationNotFound(crn, "5118f557-211e-4457-b75b-6df1f996d308")
    }

    @Test
    fun `400 from named tier calculation if calculationId is not valid`() {
        val crn = "XNOCALC"
        expectTierCalculationBadRequest(crn, "made-up-calculation-id")
    }
}
