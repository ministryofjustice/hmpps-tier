package uk.gov.justice.digital.hmpps.hmppstier.service.calculation

import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.AllPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusInputs
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh.*
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier.*
import java.math.BigDecimal
import java.time.LocalDate

object TierCalculator {
    fun calculate(deliusInputs: DeliusInputs, riskPredictors: AllPredictorDto?) = G
        .step1Reoffending(riskPredictors)
        .step2SexualReoffending(riskPredictors)
        .step3MappaAndRiskOfSeriousHarm(deliusInputs)
        .step4LiferAndImprisonmentForPublicProtection(deliusInputs)
        .step5DomesticAbuse(deliusInputs)
        .step6Stalking(deliusInputs)
        .step7ChildProtection(deliusInputs)

    private val ARP_THRESHOLDS = arrayOf(90, 75, 50, 25, 15, 0)
    private val CSRP_THRESHOLDS = arrayOf(6.9, 3.0, 1.0, 0.5, 0.0)
    private val ARP_CSRP_LOOKUP_TABLE = arrayOf(
        // CSRP  /  ARP = (90,75,50,25,15,0)
        /* 6.9+ */ arrayOf(A, A, B, B, B, B),
        /* 3.0+ */ arrayOf(A, B, C, C, C, C),
        /* 1.0+ */ arrayOf(B, C, D, E, E, E),
        /* 0.5+ */ arrayOf(C, D, E, E, F, F),
        /* 0.0+ */ arrayOf(D, D, E, F, F, G),
    )

    private fun Tier.step1Reoffending(predictors: AllPredictorDto?): Tier {
        val arp = predictors?.allReoffendingPredictor?.score ?: BigDecimal.ZERO
        val csrp = predictors?.combinedSeriousReoffendingPredictor?.score ?: BigDecimal.ZERO
        val row = CSRP_THRESHOLDS.indexOfFirst { csrp >= it.toBigDecimal() }
        val col = ARP_THRESHOLDS.indexOfFirst { arp >= it.toBigDecimal() }
        return ARP_CSRP_LOOKUP_TABLE[row][col]
    }

    private fun Tier.step2SexualReoffending(predictors: AllPredictorDto?): Tier {
        val highestSrpScore = listOfNotNull(
            predictors?.directContactSexualReoffendingPredictor?.score,
            predictors?.indirectImageContactSexualReoffendingPredictor?.score
        ).maxOrNull()

        return when {
            highestSrpScore == null -> noChange()
            highestSrpScore < 22 -> atLeast(E)
            highestSrpScore < 26 -> atLeast(D)
            highestSrpScore < 30 -> atLeast(C)
            highestSrpScore < 36 -> atLeast(B)
            else -> atLeast(A)
        }
    }

    private fun Tier.step3MappaAndRiskOfSeriousHarm(deliusInputs: DeliusInputs): Tier {
        val hasMappa = deliusInputs.registrations.mappaCategory != null
        val rosh = deliusInputs.registrations.rosh

        return if (hasMappa) {
            when (rosh) {
                VERY_HIGH -> atLeast(A)
                HIGH -> atLeast(C)
                MEDIUM -> atLeast(D)
                else -> atLeast(E)
            }
        } else {
            when (rosh) {
                VERY_HIGH -> atLeast(C)
                HIGH -> atLeast(D)
                else -> noChange()
            }
        }
    }

    private fun Tier.step4LiferAndImprisonmentForPublicProtection(deliusInputs: DeliusInputs): Tier {
        val liferIpp = deliusInputs.registrations.hasLiferIpp
        val inFirstYearOfRelease = deliusInputs.latestReleaseDate != null
            && deliusInputs.latestReleaseDate > LocalDate.now().minusYears(1)

        return when {
            liferIpp && inFirstYearOfRelease -> atLeast(B)
            liferIpp -> atLeast(F)
            else -> noChange()
        }
    }

    private fun Tier.step5DomesticAbuse(deliusInputs: DeliusInputs) =
        if (deliusInputs.registrations.hasDomesticAbuse) atLeast(E) else noChange()

    private fun Tier.step6Stalking(deliusInputs: DeliusInputs) =
        if (deliusInputs.registrations.hasStalking) atLeast(F) else noChange()

    private fun Tier.step7ChildProtection(deliusInputs: DeliusInputs) =
        if (deliusInputs.registrations.hasChildProtection) atLeast(F) else noChange()

    private fun Tier.atLeast(other: Tier) = maxOf(this, other)
    private fun Tier.noChange() = this
    private operator fun BigDecimal.compareTo(value: Int) = compareTo(value.toBigDecimal())
}