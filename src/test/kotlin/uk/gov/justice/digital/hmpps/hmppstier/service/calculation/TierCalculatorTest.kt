package uk.gov.justice.digital.hmpps.hmppstier.service.calculation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.AllPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.BasePredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.StaticOrDynamicPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.VersionedStaticOrDynamicPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusInputs
import uk.gov.justice.digital.hmpps.hmppstier.domain.Registrations
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.MappaCategory
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh.*
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier.*
import java.math.BigDecimal
import java.time.LocalDate

class TierCalculatorTest {

    @Test
    fun `defaults missing CSRP score to zero for ARP to tier mapping`() {
        assertThat(TierCalculator.calculate(deliusInputs(), predictors(arp = 90.0))).isEqualTo(D)
    }

    @Test
    fun `defaults missing ARP score to zero for CSRP to tier mapping`() {
        assertThat(TierCalculator.calculate(deliusInputs(), predictors(csrp = 3.2))).isEqualTo(C)
    }

    @ParameterizedTest(name = "ARP {0} and CSRP {1} map to tier {2}")
    @MethodSource("arpAndCsrpCombinations")
    fun `maps ARP and CSRP combinations to expected tier`(arp: Double, csrp: Double, expectedTier: Tier) {
        val tier = TierCalculator.calculate(deliusInputs(), predictors(arp = arp, csrp = csrp))
        assertThat(tier).isEqualTo(expectedTier)
    }

    @ParameterizedTest(name = "ARP {0} with CSRP 0 maps to tier {1}")
    @MethodSource("arpThresholdCases")
    fun `applies ARP threshold boundaries when CSRP is zero`(arp: Double, expectedTier: Tier) {
        val tier = TierCalculator.calculate(deliusInputs(), predictors(arp = arp, csrp = 0.0))
        assertThat(tier).isEqualTo(expectedTier)
    }

    @ParameterizedTest(name = "ARP 0 with CSRP {0} maps to tier {1}")
    @MethodSource("csrpThresholdCases")
    fun `applies CSRP threshold boundaries when ARP is zero`(csrp: Double, expectedTier: Tier) {
        val tier = TierCalculator.calculate(deliusInputs(), predictors(arp = 0.0, csrp = csrp))
        assertThat(tier).isEqualTo(expectedTier)
    }

    @Test
    fun `uses highest sexual reoffending score across direct and indirect predictors`() {
        val tier = TierCalculator.calculate(deliusInputs(), predictors(directSrp = 20.0, indirectSrp = 35.0))
        assertThat(tier).isEqualTo(B)
    }

    @Test
    fun `leaves existing tier unchanged when sexual reoffending scores are missing`() {
        val tier = TierCalculator.calculate(deliusInputs(), predictors(arp = 75.0, csrp = 1.0))
        assertThat(tier).isEqualTo(C)
    }

    @ParameterizedTest(name = "sexual reoffending score {0} maps to tier {1}")
    @MethodSource("sexualReoffendingThresholdCases")
    fun `applies sexual reoffending thresholds`(srpScore: Double, expectedTier: Tier) {
        val tier = TierCalculator.calculate(deliusInputs(), predictors(directSrp = srpScore))
        assertThat(tier).isEqualTo(expectedTier)
    }

    @ParameterizedTest(name = "mappaPresent={0} and rosh={1} map to tier {2}")
    @MethodSource("mappaAndRoshCases")
    fun `applies MAPPA and ROSH rules`(hasMappa: Boolean, rosh: Rosh?, expectedTier: Tier) {
        val tier = TierCalculator.calculate(
            deliusInputs(hasMappa = hasMappa, rosh = rosh),
            predictors(),
        )
        assertThat(tier).isEqualTo(expectedTier)
    }

    @Test
    fun `keeps existing tier when ROSH is medium and MAPPA is absent`() {
        val tier = TierCalculator.calculate(
            deliusInputs(hasMappa = false, rosh = MEDIUM),
            predictors(arp = 90.0, csrp = 1.0),
        )
        assertThat(tier).isEqualTo(B)
    }

    @ParameterizedTest(name = "hasLiferIpp={0} and latestReleaseDate={1} map to tier {2}")
    @MethodSource("liferAndReleaseDateCases")
    fun `applies lifer and release date rules`(
        hasLiferIpp: Boolean,
        latestReleaseDate: LocalDate?,
        expectedTier: Tier,
    ) {
        val tier = TierCalculator.calculate(
            deliusInputs(hasLiferIpp = hasLiferIpp, latestReleaseDate = latestReleaseDate),
            predictors(),
        )
        assertThat(tier).isEqualTo(expectedTier)
    }

    @ParameterizedTest(name = "domesticAbuse={0}, stalking={1}, childProtection={2} map to tier {3}")
    @MethodSource("registrationFlagCases")
    fun `applies domestic abuse, stalking and child protection registrations`(
        hasDomesticAbuse: Boolean,
        hasStalking: Boolean,
        hasChildProtection: Boolean,
        expectedTier: Tier,
    ) {
        val tier = TierCalculator.calculate(
            deliusInputs(
                hasDomesticAbuse = hasDomesticAbuse,
                hasStalking = hasStalking,
                hasChildProtection = hasChildProtection,
            ),
            predictors(),
        )
        assertThat(tier).isEqualTo(expectedTier)
    }

    @Test
    fun `higher tier from reoffending and sexual risk is not downgraded by later registration logic`() {
        val tier = TierCalculator.calculate(
            deliusInputs(
                hasMappa = true,
                rosh = HIGH,
                hasLiferIpp = true,
                latestReleaseDate = LocalDate.now().minusYears(2),
                hasDomesticAbuse = true,
                hasStalking = true,
                hasChildProtection = true,
            ),
            predictors(arp = 95.0, csrp = 6.9, directSrp = 22.0),
        )
        assertThat(tier).isEqualTo(A)
    }

    private fun deliusInputs(
        hasMappa: Boolean = false,
        rosh: Rosh? = null,
        hasLiferIpp: Boolean = false,
        latestReleaseDate: LocalDate? = null,
        hasDomesticAbuse: Boolean = false,
        hasStalking: Boolean = false,
        hasChildProtection: Boolean = false,
    ): DeliusInputs {
        return DeliusInputs(
            isFemale = false,
            rsrScore = BigDecimal.ZERO,
            ogrsScore = 0,
            hasNoMandate = false,
            registrations = Registrations(
                hasIomNominal = false,
                hasLiferIpp = hasLiferIpp,
                hasDomesticAbuse = hasDomesticAbuse,
                hasStalking = hasStalking,
                hasChildProtection = hasChildProtection,
                complexityFactors = emptyList(),
                rosh = rosh,
                mappaLevel = null,
                mappaCategory = if (hasMappa) MappaCategory.M1 else null,
                unsupervised = null,
            ),
            previousEnforcementActivity = false,
            latestReleaseDate = latestReleaseDate,
        )
    }

    private fun predictors(
        arp: Double? = null,
        csrp: Double? = null,
        directSrp: Double? = null,
        indirectSrp: Double? = null,
    ) = AllPredictorDto(
        allReoffendingPredictor = arp?.let { StaticOrDynamicPredictorDto(score = it.toBigDecimal()) },
        combinedSeriousReoffendingPredictor = csrp?.let { VersionedStaticOrDynamicPredictorDto(score = it.toBigDecimal()) },
        directContactSexualReoffendingPredictor = directSrp?.let { BasePredictorDto(score = it.toBigDecimal()) },
        indirectImageContactSexualReoffendingPredictor = indirectSrp?.let { BasePredictorDto(score = it.toBigDecimal()) },
    )

    companion object {
        @JvmStatic
        fun arpAndCsrpCombinations() = listOf(
            Arguments.of(75.0, 6.9, A),
            Arguments.of(75.0, 3.0, B),
            Arguments.of(75.0, 1.0, C),
            Arguments.of(25.0, 0.5, E),
            Arguments.of(25.0, 0.0, F),
        )

        @JvmStatic
        fun arpThresholdCases() = listOf(
            Arguments.of(90.0, D),
            Arguments.of(75.0, D),
            Arguments.of(74.99, E),
            Arguments.of(50.0, E),
            Arguments.of(49.99, F),
            Arguments.of(15.0, F),
            Arguments.of(14.99, G),
            Arguments.of(0.0, G),
        )

        @JvmStatic
        fun csrpThresholdCases() = listOf(
            Arguments.of(6.9, B),
            Arguments.of(3.0, C),
            Arguments.of(1.0, E),
            Arguments.of(0.5, F),
            Arguments.of(0.0, G),
        )

        @JvmStatic
        fun sexualReoffendingThresholdCases() = listOf(
            Arguments.of(21.99, E),
            Arguments.of(22.0, D),
            Arguments.of(26.0, C),
            Arguments.of(30.0, B),
            Arguments.of(36.0, A),
        )

        @JvmStatic
        fun mappaAndRoshCases() = listOf(
            Arguments.of(true, VERY_HIGH, A),
            Arguments.of(true, HIGH, C),
            Arguments.of(true, MEDIUM, D),
            Arguments.of(true, null, E),
            Arguments.of(false, VERY_HIGH, C),
            Arguments.of(false, HIGH, D),
            Arguments.of(false, MEDIUM, G),
            Arguments.of(false, null, G),
        )

        @JvmStatic
        fun liferAndReleaseDateCases(): List<Arguments> {
            val today = LocalDate.now()
            return listOf(
                Arguments.of(true, today.minusDays(1), B),
                Arguments.of(true, today.minusYears(1), F),
                Arguments.of(true, null, F),
                Arguments.of(false, null, G),
                Arguments.of(false, today.minusDays(1), G),
            )
        }

        @JvmStatic
        fun registrationFlagCases() = listOf(
            // Domestic Abuse, Stalking, Child Protection
            Arguments.of(false, false, false, G),
            Arguments.of(true, false, false, E),
            Arguments.of(false, true, false, F),
            Arguments.of(false, false, true, F),
            Arguments.of(true, true, true, E),
        )
    }
}
