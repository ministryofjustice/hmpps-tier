package uk.gov.justice.digital.hmpps.hmppstier.service.calculation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ScoreType
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.AllPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.StaticOrDynamicPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.VersionedStaticOrDynamicPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusInputs
import uk.gov.justice.digital.hmpps.hmppstier.domain.Registrations
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.MappaCategory
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier.*
import uk.gov.justice.digital.hmpps.hmppstier.service.calculation.CalculationStep.*
import java.math.BigDecimal

class ProvisionalStatusCalculatorTest {
    @Test
    fun `dynamic ARP and CSRP with RoSH present is not provisional`() {
        assertThat(
            isProvisional(
                rosh = Rosh.MEDIUM,
                predictors = predictors(arpType = ScoreType.DYNAMIC, csrpType = ScoreType.DYNAMIC),
                stepResults = stepResults(REOFFENDING to G),
            )
        ).isFalse()
    }

    @Test
    fun `both static ARP and CSRP are provisional unless another step generates tier A`() {
        val staticPredictors =
            predictors(arp = "90.0", csrp = "6.9", arpType = ScoreType.STATIC, csrpType = ScoreType.STATIC)

        assertThat(
            isProvisional(
                rosh = Rosh.HIGH,
                predictors = staticPredictors,
                stepResults = stepResults(REOFFENDING to A, MAPPA_ROSH to D),
            )
        ).isTrue()

        assertThat(
            isProvisional(
                rosh = Rosh.HIGH,
                predictors = staticPredictors,
                stepResults = stepResults(REOFFENDING to A, SEXUAL_REOFFENDING to A),
            )
        ).isFalse()

        assertThat(
            isProvisional(
                rosh = Rosh.HIGH,
                predictors = staticPredictors,
                stepResults = stepResults(REOFFENDING to A, MAPPA_ROSH to A),
            )
        ).isFalse()
    }

    @Test
    fun `static ARP with dynamic CSRP is provisional until another step reaches the maximum possible ARP CSRP tier`() {
        val predictors = predictors(arpType = ScoreType.STATIC, csrpType = ScoreType.DYNAMIC, csrp = "0.5")

        assertThat(
            isProvisional(
                rosh = Rosh.MEDIUM,
                predictors = predictors,
                stepResults = stepResults(REOFFENDING to F, SEXUAL_OFFENCES to E),
            )
        ).isTrue()

        assertThat(
            isProvisional(
                rosh = Rosh.MEDIUM,
                predictors = predictors,
                stepResults = stepResults(REOFFENDING to F, MAPPA_ROSH to C),
            )
        ).isFalse()
    }

    @Test
    fun `dynamic ARP with static CSRP is provisional until another step reaches the maximum possible ARP CSRP tier`() {
        val predictors = predictors(arp = "25.0", arpType = ScoreType.DYNAMIC, csrpType = ScoreType.STATIC)

        assertThat(
            isProvisional(
                rosh = Rosh.MEDIUM,
                predictors = predictors,
                stepResults = stepResults(REOFFENDING to F, STALKING to F),
            )
        ).isTrue()

        assertThat(
            isProvisional(
                rosh = Rosh.MEDIUM,
                predictors = predictors,
                stepResults = stepResults(REOFFENDING to F, SEXUAL_REOFFENDING to B),
            )
        ).isFalse()
    }

    @Test
    fun `missing RoSH with MAPPA is provisional unless dynamic ARP CSRP or sexual reoffending generates tier A`() {
        assertThat(
            isProvisional(
                rosh = null,
                mappa = MappaCategory.M1,
                predictors = predictors(arp = "50.0", csrp = "6.9"),
                stepResults = stepResults(REOFFENDING to B),
            )
        ).isTrue()

        assertThat(
            isProvisional(
                rosh = null,
                mappa = MappaCategory.M1,
                predictors = predictors(arp = "90.0", csrp = "6.9"),
                stepResults = stepResults(REOFFENDING to A),
            )
        ).isFalse()

        assertThat(
            isProvisional(
                rosh = null,
                mappa = MappaCategory.M1,
                predictors = predictors(),
                stepResults = stepResults(REOFFENDING to G, SEXUAL_REOFFENDING to A),
            )
        ).isFalse()

        assertThat(
            isProvisional(
                rosh = null,
                mappa = MappaCategory.M1,
                predictors = predictors(arp = "90.0", csrp = "0.5"),
                stepResults = stepResults(REOFFENDING to C, LIFER_IPP to B, SEXUAL_REOFFENDING to C),
            )
        ).isTrue()
    }

    @Test
    fun `missing RoSH without MAPPA is provisional unless dynamic ARP CSRP generates tier C or higher`() {
        assertThat(
            isProvisional(
                rosh = null,
                predictors = predictors(arp = "75.0", csrp = "0.0"),
                stepResults = stepResults(REOFFENDING to D),
            )
        ).isTrue()

        assertThat(
            isProvisional(
                rosh = null,
                predictors = predictors(arp = "90.0", csrp = "0.5"),
                stepResults = stepResults(REOFFENDING to C),
            )
        ).isFalse()

        assertThat(
            isProvisional(
                rosh = null,
                predictors = predictors(arp = "50.0", csrp = "6.9"),
                stepResults = stepResults(REOFFENDING to B),
            )
        ).isFalse()
    }

    @Test
    fun `missing RoSH without MAPPA is provisional unless sexual reoffending generates tier C or higher`() {
        assertThat(
            isProvisional(
                rosh = null,
                predictors = predictors(),
                stepResults = stepResults(REOFFENDING to G, SEXUAL_REOFFENDING to D),
            )
        ).isTrue()

        assertThat(
            isProvisional(
                rosh = null,
                predictors = predictors(),
                stepResults = stepResults(REOFFENDING to G, SEXUAL_REOFFENDING to C),
            )
        ).isFalse()
    }

    @Test
    fun `missing RoSH without MAPPA is provisional unless recent lifer release generates tier B`() {
        assertThat(
            isProvisional(
                rosh = null,
                predictors = predictors(),
                stepResults = stepResults(REOFFENDING to G, LIFER_IPP to D),
            )
        ).isTrue()

        assertThat(
            isProvisional(
                rosh = null,
                predictors = predictors(),
                stepResults = stepResults(REOFFENDING to G, LIFER_IPP to B),
            )
        ).isFalse()
    }

    @Test
    fun `missing RoSH ignores reoffending when either ARP or CSRP is static`() {
        assertThat(
            isProvisional(
                rosh = null,
                predictors = predictors(
                    arp = "90.0",
                    csrp = "6.9",
                    arpType = ScoreType.STATIC,
                    csrpType = ScoreType.STATIC
                ),
                stepResults = stepResults(REOFFENDING to A),
            )
        ).isTrue()

        assertThat(
            isProvisional(
                rosh = null,
                predictors = predictors(arp = "90.0", csrp = "6.9", arpType = ScoreType.STATIC),
                stepResults = stepResults(REOFFENDING to A),
            )
        ).isTrue()

        assertThat(
            isProvisional(
                rosh = null,
                predictors = predictors(csrp = "6.9", csrpType = ScoreType.STATIC),
                stepResults = stepResults(REOFFENDING to A),
            )
        ).isTrue()
    }

    @Test
    fun `missing RoSH ignores the MAPPA RoSH step when determining provisional status`() {
        assertThat(
            isProvisional(
                rosh = null,
                mappa = MappaCategory.M1,
                predictors = predictors(),
                stepResults = stepResults(MAPPA_ROSH to A),
            )
        ).isTrue()
    }

    @Test
    fun `missing RoSH remains provisional when other lower factors generate the highest tier`() {
        assertThat(
            isProvisional(
                rosh = null,
                predictors = predictors(),
                stepResults = stepResults(REOFFENDING to G, SEXUAL_OFFENCES to E, DOMESTIC_ABUSE to E),
            )
        ).isTrue()
    }

    private fun isProvisional(
        rosh: Rosh?,
        mappa: MappaCategory? = null,
        predictors: AllPredictorDto,
        stepResults: Map<CalculationStep, Tier?>,
    ) = ProvisionalStatusCalculator.isProvisional(deliusInputs(rosh, mappa), predictors, stepResults)

    private fun deliusInputs(rosh: Rosh?, mappa: MappaCategory?) = DeliusInputs(
        isFemale = false,
        rsrScore = BigDecimal.ZERO,
        ogrsScore = 0,
        hasNoMandate = false,
        registrations = Registrations(
            hasIomNominal = false,
            hasLiferIpp = false,
            hasDomesticAbuse = false,
            hasStalking = false,
            hasChildProtection = false,
            complexityFactors = emptyList(),
            rosh = rosh,
            mappaLevel = null,
            mappaCategory = mappa,
            unsupervised = null,
        ),
        previousEnforcementActivity = false,
        latestReleaseDate = null,
        hasActiveEvent = true,
    )

    private fun predictors(
        arp: String = "0.0",
        csrp: String = "0.0",
        arpType: ScoreType = ScoreType.DYNAMIC,
        csrpType: ScoreType = ScoreType.DYNAMIC,
    ) = AllPredictorDto(
        allReoffendingPredictor = StaticOrDynamicPredictorDto(
            staticOrDynamic = arpType,
            score = BigDecimal(arp),
        ),
        combinedSeriousReoffendingPredictor = VersionedStaticOrDynamicPredictorDto(
            staticOrDynamic = csrpType,
            score = BigDecimal(csrp),
        ),
    )

    private fun stepResults(vararg results: Pair<CalculationStep, Tier?>) = mapOf(*results)
}
