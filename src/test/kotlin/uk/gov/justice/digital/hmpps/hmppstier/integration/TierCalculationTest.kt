package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.arnsApi.ArnsApiExtension.Companion.arnsApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.AssessmentApiExtension.Companion.assessmentApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.tierToDeliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Registration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.TierDetails
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase

class TierCalculationTest : IntegrationTestBase() {

    @Test
    fun `no NSis returned Female Offender`() {
        val crn = "X386786"
        arnsApi.getNotFoundAssessment(crn)
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

        restOfSetupWithFemaleOffender(crn, 2234567890)

        calculateTierFor(crn)
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
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, false, 4234568890)
        arnsApi.getOutdatedAssessment(crn, 1234567890)

        calculateTierFor(crn)
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
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, false, 4234568890)
        arnsApi.getOutdatedAssessment(crn, 1234567890)

        calculateTierFor(crn)
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
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, false, 4234568890)
        arnsApi.getOutdatedAssessment(crn, 1234567890)

        calculateTierFor(crn)
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
        arnsApi.getHighSeverityNeeds(crn)
        arnsApi.getCurrentAssessment(crn, 4234568899) // assessment not out of date

        calculateTierFor(crn)
        expectTierChangedById("A1")
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
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, false, 4234568890)
        arnsApi.getOutdatedAssessment(crn, 4234568890)

        calculateTierFor(crn)
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
        restOfSetupWithMaleOffenderNoSevereNeeds(crn, false, 4234568891)
        arnsApi.getCurrentAssessment(crn, 4234568891) // assessment not out of date
        calculateTierFor(crn)
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

        calculateTierFor(crn)
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

        calculateTierFor(crn)
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

        calculateTierFor(crn)
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

        calculateTierFor(crn)
        expectLatestTierCalculation("B1")
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
