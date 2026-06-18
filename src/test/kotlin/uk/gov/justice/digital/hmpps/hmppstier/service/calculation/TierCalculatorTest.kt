package uk.gov.justice.digital.hmpps.hmppstier.service.calculation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.OGRS4Predictors
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ScoreLevel
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ScoreLevel.*
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ScoreType
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.AllPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.BasePredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.StaticOrDynamicPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.VersionedStaticOrDynamicPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusInputs
import uk.gov.justice.digital.hmpps.hmppstier.domain.OASysInputs
import uk.gov.justice.digital.hmpps.hmppstier.domain.Registrations
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.MappaCategory
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class TierCalculatorTest {

    @Test
    fun `cases without an active event have a tier of NOT_SUPERVISED`() {
        assertThat(TierCalculator.calculate(deliusInputs(hasActiveEvent = false), oasysInputs()).tier)
            .isEqualTo(NOT_SUPERVISED)
    }

    @Test
    fun `missing CSRP score has a tier of MISSING`() {
        assertThat(TierCalculator.calculate(deliusInputs(), oasysInputs(arp = 90.0, csrp = null)).tier)
            .isEqualTo(MISSING)
    }

    @Test
    fun `missing ARP score has a tier of MISSING`() {
        assertThat(TierCalculator.calculate(deliusInputs(), oasysInputs(arp = null, csrp = 3.2)).tier)
            .isEqualTo(MISSING)
    }

    @ParameterizedTest(name = "ARP {0} and CSRP {1} map to tier {2}")
    @MethodSource("arpAndCsrpMatrixCases")
    fun `maps every ARP and CSRP lookup-table combination`(arp: Double, csrp: Double, expectedTier: Tier) {
        assertThat(TierCalculator.calculate(deliusInputs(), oasysInputs(arp = arp, csrp = csrp)).tier)
            .isEqualTo(expectedTier)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ignoredSexualPredictorCases")
    fun `ignores sexual predictors without a usable validated band`(
        description: String,
        directSrp: BasePredictorDto?,
    ) {
        val tier = TierCalculator.calculate(
            deliusInputs(),
            oasysInputs(
                arp = 75.0,
                csrp = 1.0,
                directSrp = directSrp,
            ),
        ).tier

        assertThat(tier)
            .describedAs(description)
            .isEqualTo(C)
    }

    @ParameterizedTest(name = "direct score {0} with band {1} maps to tier {2}")
    @MethodSource("directSexualReoffendingCases")
    fun `applies direct-contact sexual reoffending rules`(score: Double, band: ScoreLevel, expectedTier: Tier) {
        val tier = TierCalculator.calculate(
            deliusInputs(),
            oasysInputs(directSrp = sexualPredictor(score, band)),
        ).tier

        assertThat(tier).isEqualTo(expectedTier)
    }

    @Test
    fun `throws when a direct-contact medium-band predictor is below the supported thresholds`() {
        assertThrows(IllegalStateException::class.java) {
            TierCalculator.calculate(
                deliusInputs(),
                oasysInputs(directSrp = sexualPredictor(0.59, MEDIUM)),
            )
        }
    }

    @ParameterizedTest(name = "mappaPresent={0} and rosh={1} map to tier {2}")
    @MethodSource("mappaAndRoshCases")
    fun `applies MAPPA and ROSH rules`(hasMappa: Boolean, rosh: Rosh?, expectedTier: Tier) {
        val tier = TierCalculator.calculate(
            deliusInputs(hasMappa = hasMappa, rosh = rosh),
            oasysInputs(),
        ).tier
        assertThat(tier).isEqualTo(expectedTier)
    }

    @Test
    fun `keeps existing tier when ROSH is medium and MAPPA is absent`() {
        val tier = TierCalculator.calculate(
            deliusInputs(hasMappa = false, rosh = Rosh.MEDIUM),
            oasysInputs(arp = 90.0, csrp = 1.0),
        ).tier
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
            oasysInputs(),
        ).tier
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
            oasysInputs(),
        ).tier
        assertThat(tier).isEqualTo(expectedTier)
    }

    @Test
    fun `applies people convicted of sexual offences rule`() {
        assertThat(
            TierCalculator.calculate(
                deliusInputs(rosh = Rosh.MEDIUM),
                oasysInputs(everCommittedSexualOffence = true),
            )
        ).isEqualTo(CalculationResult(E))
    }

    @Test
    fun `higher tier from earlier rules is not downgraded by later registration logic`() {
        val tier = TierCalculator.calculate(
            deliusInputs(
                hasMappa = true,
                rosh = Rosh.HIGH,
                hasLiferIpp = true,
                latestReleaseDate = LocalDate.now().minusYears(2),
                hasDomesticAbuse = true,
                hasStalking = true,
                hasChildProtection = true,
            ),
            oasysInputs(arp = 95.0, csrp = 6.9, directSrp = sexualPredictor(5.31, VERY_HIGH)),
        ).tier
        assertThat(tier).isEqualTo(A)
    }

    @Test
    fun `both static ARP and CSRP are provisional unless DC-SRP or MAPPA and ROSH generates tier A`() {
        assertThat(
            TierCalculator.calculate(
                deliusInputs(rosh = Rosh.HIGH),
                oasysInputs(arp = 90.0, csrp = 6.9, arpType = ScoreType.STATIC, csrpType = ScoreType.STATIC),
            )
        ).isEqualTo(CalculationResult(A, provisional = true))

        assertThat(
            TierCalculator.calculate(
                deliusInputs(rosh = Rosh.HIGH),
                oasysInputs(
                    arp = 90.0,
                    csrp = 6.9,
                    arpType = ScoreType.STATIC,
                    csrpType = ScoreType.STATIC,
                    directSrp = sexualPredictor(0.01, VERY_HIGH),
                ),
            )
        ).isEqualTo(CalculationResult(A, provisional = false))

        assertThat(
            TierCalculator.calculate(
                deliusInputs(hasMappa = true, rosh = Rosh.VERY_HIGH),
                oasysInputs(arp = 90.0, csrp = 6.9, arpType = ScoreType.STATIC, csrpType = ScoreType.STATIC),
            )
        ).isEqualTo(CalculationResult(A, provisional = false))
    }

    @Test
    fun `static ARP with dynamic CSRP is provisional unless another factor reaches the maximum ARP CSRP tier`() {
        assertThat(
            TierCalculator.calculate(
                deliusInputs(rosh = Rosh.MEDIUM),
                oasysInputs(arp = 0.0, csrp = 0.5, arpType = ScoreType.STATIC, csrpType = ScoreType.DYNAMIC),
            )
        ).isEqualTo(CalculationResult(F, provisional = true))

        assertThat(
            TierCalculator.calculate(
                deliusInputs(hasMappa = true, rosh = Rosh.HIGH),
                oasysInputs(arp = 0.0, csrp = 0.5, arpType = ScoreType.STATIC, csrpType = ScoreType.DYNAMIC),
            )
        ).isEqualTo(CalculationResult(C, provisional = false))
    }

    @Test
    fun `dynamic ARP with static CSRP is provisional unless another factor reaches the maximum ARP CSRP tier`() {
        assertThat(
            TierCalculator.calculate(
                deliusInputs(rosh = Rosh.MEDIUM),
                oasysInputs(arp = 25.0, csrp = 0.0, arpType = ScoreType.DYNAMIC, csrpType = ScoreType.STATIC),
            )
        ).isEqualTo(CalculationResult(F, provisional = true))

        assertThat(
            TierCalculator.calculate(
                deliusInputs(rosh = Rosh.MEDIUM),
                oasysInputs(
                    arp = 25.0,
                    csrp = 0.0,
                    arpType = ScoreType.DYNAMIC,
                    csrpType = ScoreType.STATIC,
                    directSrp = sexualPredictor(2.11, HIGH),
                ),
            )
        ).isEqualTo(CalculationResult(B, provisional = false))
    }

    @Test
    fun `missing ROSH with MAPPA is provisional unless dynamic ARP CSRP or DC-SRP generates tier A`() {
        assertThat(
            TierCalculator.calculate(
                deliusInputs(hasMappa = true, rosh = null),
                oasysInputs(arp = 50.0, csrp = 6.9)
            )
        )
            .isEqualTo(CalculationResult(B, provisional = true))

        assertThat(
            TierCalculator.calculate(
                deliusInputs(hasMappa = true, rosh = null),
                oasysInputs(arp = 90.0, csrp = 6.9)
            )
        )
            .isEqualTo(CalculationResult(A, provisional = false))

        assertThat(
            TierCalculator.calculate(
                deliusInputs(hasMappa = true, rosh = null),
                oasysInputs(arp = 0.0, csrp = 0.0, directSrp = sexualPredictor(0.01, VERY_HIGH)),
            )
        ).isEqualTo(CalculationResult(A, provisional = false))

        assertThat(
            TierCalculator.calculate(
                deliusInputs(hasMappa = true, rosh = null, hasLiferIpp = true, latestReleaseDate = LocalDate.now()),
                oasysInputs(arp = 90.0, csrp = 0.5, directSrp = sexualPredictor(1.12, MEDIUM)),
            )
        ).isEqualTo(CalculationResult(B, provisional = true))
    }

    @Test
    fun `missing ROSH without MAPPA is provisional unless dynamic ARP CSRP or DC-SRP reaches tier C or lifer reaches tier B`() {
        assertThat(TierCalculator.calculate(deliusInputs(rosh = null), oasysInputs(arp = 75.0, csrp = 0.0)))
            .isEqualTo(CalculationResult(D, provisional = true))

        assertThat(TierCalculator.calculate(deliusInputs(rosh = null), oasysInputs(arp = 90.0, csrp = 0.5)))
            .isEqualTo(CalculationResult(C, provisional = false))

        assertThat(TierCalculator.calculate(deliusInputs(rosh = null), oasysInputs(arp = 50.0, csrp = 6.9)))
            .isEqualTo(CalculationResult(B, provisional = false))

        assertThat(
            TierCalculator.calculate(
                deliusInputs(rosh = null),
                oasysInputs(arp = 0.0, csrp = 0.0, directSrp = sexualPredictor(1.12, MEDIUM)),
            )
        ).isEqualTo(CalculationResult(C, provisional = false))

        assertThat(
            TierCalculator.calculate(
                deliusInputs(rosh = null),
                oasysInputs(arp = 0.0, csrp = 0.0, directSrp = sexualPredictor(0.60, MEDIUM)),
            )
        ).isEqualTo(CalculationResult(D, provisional = true))

        assertThat(
            TierCalculator.calculate(
                deliusInputs(rosh = null, hasLiferIpp = true, latestReleaseDate = LocalDate.now()),
                oasysInputs(),
            )
        ).isEqualTo(CalculationResult(B, provisional = false))

        assertThat(TierCalculator.calculate(deliusInputs(rosh = null), oasysInputs(everCommittedSexualOffence = true)))
            .isEqualTo(CalculationResult(E, provisional = true))
    }

    private fun deliusInputs(
        hasMappa: Boolean = false,
        rosh: Rosh? = null,
        hasLiferIpp: Boolean = false,
        latestReleaseDate: LocalDate? = null,
        hasDomesticAbuse: Boolean = false,
        hasStalking: Boolean = false,
        hasChildProtection: Boolean = false,
        hasActiveEvent: Boolean = true,
    ) = DeliusInputs(
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
        hasActiveEvent = hasActiveEvent,
    )

    private fun oasysInputs(
        arp: Double? = 0.0,
        csrp: Double? = 0.0,
        arpType: ScoreType? = ScoreType.DYNAMIC,
        csrpType: ScoreType? = ScoreType.DYNAMIC,
        arpBand: ScoreLevel? = LOW,
        csrpBand: ScoreLevel? = LOW,
        directSrp: BasePredictorDto? = null,
        everCommittedSexualOffence: Boolean = false,
    ) = OASysInputs(
        predictors = OGRS4Predictors(
            completedDate = LocalDateTime.of(2026, 6, 9, 12, 0, 0),
            output = AllPredictorDto(
                allReoffendingPredictor = arp?.let {
                    StaticOrDynamicPredictorDto(
                        staticOrDynamic = arpType,
                        score = it.toBigDecimal(),
                        band = arpBand,
                    )
                },
                combinedSeriousReoffendingPredictor = csrp?.let {
                    VersionedStaticOrDynamicPredictorDto(
                        staticOrDynamic = csrpType,
                        score = it.toBigDecimal(),
                        band = csrpBand,
                    )
                },
                directContactSexualReoffendingPredictor = directSrp,
            )
        ),
        everCommittedSexualOffence = everCommittedSexualOffence,
    )

    private fun sexualPredictor(score: Double, band: ScoreLevel? = null) = BasePredictorDto(
        score = score.toBigDecimal(),
        band = band,
    )

    companion object {
        @JvmStatic
        fun arpAndCsrpMatrixCases() = listOf(
            Arguments.of(90.0, 6.9, A),
            Arguments.of(75.0, 6.9, A),
            Arguments.of(50.0, 6.9, B),
            Arguments.of(25.0, 6.9, B),
            Arguments.of(15.0, 6.9, B),
            Arguments.of(0.0, 6.9, B),
            Arguments.of(90.0, 3.0, A),
            Arguments.of(75.0, 3.0, B),
            Arguments.of(50.0, 3.0, C),
            Arguments.of(25.0, 3.0, C),
            Arguments.of(15.0, 3.0, C),
            Arguments.of(0.0, 3.0, C),
            Arguments.of(90.0, 1.0, B),
            Arguments.of(75.0, 1.0, C),
            Arguments.of(50.0, 1.0, D),
            Arguments.of(25.0, 1.0, E),
            Arguments.of(15.0, 1.0, E),
            Arguments.of(0.0, 1.0, E),
            Arguments.of(90.0, 0.5, C),
            Arguments.of(75.0, 0.5, D),
            Arguments.of(50.0, 0.5, E),
            Arguments.of(25.0, 0.5, E),
            Arguments.of(15.0, 0.5, F),
            Arguments.of(0.0, 0.5, F),
            Arguments.of(90.0, 0.0, D),
            Arguments.of(75.0, 0.0, D),
            Arguments.of(50.0, 0.0, E),
            Arguments.of(25.0, 0.0, F),
            Arguments.of(15.0, 0.0, F),
            Arguments.of(0.0, 0.0, G),
        )

        @JvmStatic
        fun ignoredSexualPredictorCases() = listOf(
            Arguments.of(
                "direct predictor without a band is ignored",
                BasePredictorDto(score = BigDecimal("2.11")),
            ),
            Arguments.of(
                "direct predictor with NOT_APPLICABLE band is ignored",
                BasePredictorDto(score = BigDecimal("2.11"), band = NOT_APPLICABLE),
            ),
        )

        @JvmStatic
        fun directSexualReoffendingCases() = listOf(
            Arguments.of(5.31, VERY_HIGH, A),
            Arguments.of(0.01, VERY_HIGH, A),
            Arguments.of(2.11, HIGH, B),
            Arguments.of(1.50, HIGH, B),
            Arguments.of(5.31, HIGH, B),
            Arguments.of(1.12, MEDIUM, C),
            Arguments.of(3.36, MEDIUM, C),
            Arguments.of(2.11, MEDIUM, D),
            Arguments.of(0.60, MEDIUM, D),
            Arguments.of(0.02, LOW, E),
        )

        @JvmStatic
        fun mappaAndRoshCases() = listOf(
            Arguments.of(true, Rosh.VERY_HIGH, A),
            Arguments.of(true, Rosh.HIGH, C),
            Arguments.of(true, Rosh.MEDIUM, D),
            Arguments.of(true, Rosh.LOW, E),
            Arguments.of(true, null, E),
            Arguments.of(false, Rosh.VERY_HIGH, C),
            Arguments.of(false, Rosh.HIGH, D),
            Arguments.of(false, Rosh.MEDIUM, G),
            Arguments.of(false, Rosh.LOW, G),
            Arguments.of(false, null, G),
        )

        @JvmStatic
        fun liferAndReleaseDateCases(): List<Arguments> {
            val today = LocalDate.now()
            return listOf(
                Arguments.of(true, today.minusDays(1), B),
                Arguments.of(true, today.minusYears(1).plusDays(1), B),
                Arguments.of(true, today.minusYears(1), B),
                Arguments.of(true, today.minusYears(1).minusDays(1), D),
                Arguments.of(true, today.minusYears(2), D),
                Arguments.of(true, today.minusYears(5).plusDays(1), D),
                Arguments.of(true, today.minusYears(5), D),
                Arguments.of(true, today.minusYears(5).minusDays(1), E),
                Arguments.of(true, today.minusYears(6), E),
                Arguments.of(true, null, G),
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
            Arguments.of(true, true, false, E),
            Arguments.of(true, false, true, E),
            Arguments.of(false, true, true, F),
            Arguments.of(true, true, true, E),
        )
    }
}
