package uk.gov.justice.digital.hmpps.hmppstier.service.calculation

import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ScoreLevel.*
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.AllPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.ValidPredictor.Companion.validate
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusInputs
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier.*
import java.math.BigDecimal
import java.math.BigDecimal.ZERO

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
        val arp = predictors?.allReoffendingPredictor?.score ?: ZERO
        val csrp = predictors?.combinedSeriousReoffendingPredictor?.score ?: ZERO
        val row = CSRP_THRESHOLDS.indexOfFirst { csrp >= it }
        val col = ARP_THRESHOLDS.indexOfFirst { arp >= it }
        return ARP_CSRP_LOOKUP_TABLE[row][col]
    }

    private fun Tier.step2SexualReoffending(predictors: AllPredictorDto?): Tier {
        val dc = predictors?.directContactSexualReoffendingPredictor.validate()
        val iic = predictors?.indirectImageContactSexualReoffendingPredictor.validate()
        return if (dc != null && (iic == null || dc.score >= iic.score)) when {
            // without risk reduction
            dc.score >= 5.31 && dc.band == VERY_HIGH -> atLeast(A)
            dc.score >= 2.11 && dc.band == HIGH -> atLeast(B)
            dc.score >= 1.12 && dc.band == MEDIUM -> atLeast(C)
            dc.score >= 0.60 && dc.band == MEDIUM -> atLeast(D)
            dc.score >= 0.02 && dc.band == LOW -> atLeast(D)
            // with risk reduction
            dc.score >= 5.31 && dc.band == HIGH -> atLeast(B)
            dc.score >= 3.36 && dc.band == MEDIUM -> atLeast(C)
            dc.score >= 2.11 && dc.band == MEDIUM -> atLeast(D)
            dc.score >= 0.02 && dc.band == LOW -> atLeast(E)
            else -> error("Unexpected combination of DC-SRP score and band")
        } else if (iic != null) when {
            iic.band >= HIGH -> atLeast(C)
            iic.band >= MEDIUM -> atLeast(D)
            else -> atLeast(E)
        } else noChange()
    }

    private fun Tier.step3MappaAndRiskOfSeriousHarm(deliusInputs: DeliusInputs): Tier {
        val hasMappa = deliusInputs.registrations.mappaCategory != null
        val rosh = deliusInputs.registrations.rosh
        return if (hasMappa) when (rosh) {
            Rosh.VERY_HIGH -> atLeast(A)
            Rosh.HIGH -> atLeast(C)
            Rosh.MEDIUM -> atLeast(D)
            else -> atLeast(E)
        } else when (rosh) {
            Rosh.VERY_HIGH -> atLeast(C)
            Rosh.HIGH -> atLeast(D)
            else -> noChange()
        }
    }

    private fun Tier.step4LiferAndImprisonmentForPublicProtection(deliusInputs: DeliusInputs): Tier {
        val liferIpp = deliusInputs.registrations.hasLiferIpp
        return when {
            liferIpp && deliusInputs.inFirstYearOfRelease() -> atLeast(B)
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
    private operator fun BigDecimal.compareTo(value: Double) = compareTo(value.toBigDecimal())
}