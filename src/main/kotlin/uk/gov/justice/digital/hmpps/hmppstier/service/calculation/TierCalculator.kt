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
    fun calculate(deliusInputs: DeliusInputs, riskPredictors: AllPredictorDto?) = maxOfNotNull(
        nonSexualReoffending(riskPredictors),
        sexualReoffending(riskPredictors),
        mappaAndRiskOfSeriousHarm(deliusInputs),
        liferAndImprisonmentForPublicProtection(deliusInputs),
        domesticAbuse(deliusInputs),
        stalking(deliusInputs),
        childProtection(deliusInputs),
    ) ?: G

    fun nonSexualReoffending(riskPredictors: AllPredictorDto?) = riskPredictors?.run {
        val arp = allReoffendingPredictor?.score ?: ZERO
        val dcSrp = directContactSexualReoffendingPredictor?.validate()?.score
        val iicSrp = indirectImageContactSexualReoffendingPredictor?.validate()?.score
        // The Combined Serious Reoffending Predictor (CSRP) is a combination of multiple scores, including the
        // sexual reoffending predictors (DC-SRP and IIC-SRP), so should not be used if DC-SRP and IIC-SRP are both
        // valid and IIC-SRP is greater than DC-SRP. Otherwise, we would double-count sexual reoffending in step 2.
        val suppressCsrp = iicSrp != null && dcSrp != null && iicSrp > dcSrp
        val csrp = combinedSeriousReoffendingPredictor?.score?.takeUnless { suppressCsrp } ?: ZERO
        val row = arrayOf(6.9, 3.0, 1.0, 0.5, 0.0).indexOfFirst { csrp >= it }
        val col = arrayOf(90, 75, 50, 25, 15, 0).indexOfFirst { arp >= it }
        arrayOf(
            // CSRP  /  ARP = (90,75,50,25,15,0)
            /* 6.9+ */ arrayOf(A, A, B, B, B, B),
            /* 3.0+ */ arrayOf(A, B, C, C, C, C),
            /* 1.0+ */ arrayOf(B, C, D, E, E, E),
            /* 0.5+ */ arrayOf(C, D, E, E, F, F),
            /* 0.0+ */ arrayOf(D, D, E, F, F, G),
        )[row][col]
    }

    fun sexualReoffending(riskPredictors: AllPredictorDto?) = riskPredictors?.run {
        val dc = directContactSexualReoffendingPredictor.validate()
        val iic = indirectImageContactSexualReoffendingPredictor.validate()
        if (dc != null && (iic == null || dc.score >= iic.score)) when {
            dc.band >= VERY_HIGH -> A
            dc.band >= HIGH -> B
            dc.band >= MEDIUM -> when {
                // without risk reduction
                dc.score >= 3.36 -> C
                dc.score >= 2.11 -> D
                // with risk reduction
                dc.score >= 1.12 -> C
                dc.score >= 0.60 -> D
                else -> error("Unexpected combination of DC-SRP score and band")
            }

            else -> E
        } else if (iic != null) when {
            iic.band >= HIGH -> C
            iic.band >= MEDIUM -> D
            else -> E
        } else null
    }

    fun mappaAndRiskOfSeriousHarm(deliusInputs: DeliusInputs) = with(deliusInputs.registrations) {
        if (mappaCategory != null) when (rosh) {
            Rosh.VERY_HIGH -> A
            Rosh.HIGH -> C
            Rosh.MEDIUM -> D
            else -> E
        } else when (rosh) {
            Rosh.VERY_HIGH -> C
            Rosh.HIGH -> D
            else -> null
        }
    }

    fun liferAndImprisonmentForPublicProtection(deliusInputs: DeliusInputs) = when {
        deliusInputs.registrations.hasLiferIpp && deliusInputs.inFirstYearOfRelease() -> B
        deliusInputs.registrations.hasLiferIpp -> F
        else -> null
    }

    fun domesticAbuse(deliusInputs: DeliusInputs) = E.takeIf { deliusInputs.registrations.hasDomesticAbuse }
    fun stalking(deliusInputs: DeliusInputs) = F.takeIf { deliusInputs.registrations.hasStalking }
    fun childProtection(deliusInputs: DeliusInputs) = F.takeIf { deliusInputs.registrations.hasChildProtection }

    private fun maxOfNotNull(vararg values: Tier?) = values.filterNotNull().maxOrNull()
    private operator fun BigDecimal.compareTo(value: Int) = compareTo(value.toBigDecimal())
    private operator fun BigDecimal.compareTo(value: Double) = compareTo(value.toBigDecimal())
}
