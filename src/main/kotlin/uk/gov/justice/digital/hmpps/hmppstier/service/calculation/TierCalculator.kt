package uk.gov.justice.digital.hmpps.hmppstier.service.calculation

import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.AllPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.ValidPredictor.Companion.validate
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.ValidPredictor.ValidScoreLevel.*
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusInputs
import uk.gov.justice.digital.hmpps.hmppstier.domain.OASysInputs
import uk.gov.justice.digital.hmpps.hmppstier.domain.Registrations
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier.*
import uk.gov.justice.digital.hmpps.hmppstier.service.calculation.CalculationStep.*
import uk.gov.justice.digital.hmpps.hmppstier.service.calculation.ProvisionalStatusCalculator.isProvisional
import uk.gov.justice.digital.hmpps.hmppstier.service.calculation.ReoffendingPredictorTable.compareTo
import java.math.BigDecimal.ZERO
import java.time.LocalDate

object TierCalculator {
    fun calculate(deliusInputs: DeliusInputs, oasysInputs: OASysInputs?): CalculationResult {
        if (!deliusInputs.hasActiveEvent) return CalculationResult(NOT_SUPERVISED)
        if (oasysInputs == null || !oasysInputs.hasArpAndCsrp) return CalculationResult(MISSING)

        val stepResults = mapOf(
            REOFFENDING to oasysInputs.predictors.nonSexualReoffending(),
            SEXUAL_REOFFENDING to oasysInputs.predictors.sexualReoffending(),
            MAPPA_ROSH to deliusInputs.registrations.mappaAndRiskOfSeriousHarm(),
            LIFER_IPP to deliusInputs.liferAndImprisonmentForPublicProtection(),
            DOMESTIC_ABUSE to deliusInputs.registrations.domesticAbuse(),
            STALKING to deliusInputs.registrations.stalking(),
            CHILD_PROTECTION to deliusInputs.registrations.childProtection(),
            SEXUAL_OFFENCES to oasysInputs.sexualOffences(),
        )
        return CalculationResult(
            tier = stepResults.maxOf { it.value ?: G },
            provisional = isProvisional(deliusInputs, oasysInputs.predictors, stepResults)
        )
    }

    fun AllPredictorDto.nonSexualReoffending() = ReoffendingPredictorTable.calculate(
        arp = allReoffendingPredictor?.score ?: ZERO,
        csrp = combinedSeriousReoffendingPredictor?.score ?: ZERO
    )

    fun AllPredictorDto.sexualReoffending(): Tier? =
        with(directContactSexualReoffendingPredictor.validate() ?: return null) {
            when (band) {
                VERY_HIGH -> A
                HIGH -> B
                MEDIUM -> when {
                    // without risk reduction
                    score >= 3.36 -> C
                    score >= 2.11 -> D
                    // with risk reduction
                    score >= 1.12 -> C
                    score >= 0.60 -> D
                    else -> error("Unexpected combination of DC-SRP score and band")
                }

                LOW -> E
            }
        }

    fun Registrations.mappaAndRiskOfSeriousHarm() = if (mappaCategory != null) when (rosh) {
        Rosh.VERY_HIGH -> A
        Rosh.HIGH -> C
        Rosh.MEDIUM -> D
        Rosh.LOW -> E
        else -> null
    } else when (rosh) {
        Rosh.VERY_HIGH -> C
        Rosh.HIGH -> D
        else -> null
    }

    fun DeliusInputs.liferAndImprisonmentForPublicProtection(): Tier? {
        val today = LocalDate.now()
        return when {
            latestReleaseDate == null || !registrations.hasLiferIpp -> null
            latestReleaseDate >= today.minusYears(1) -> B
            latestReleaseDate >= today.minusYears(5) -> D
            else -> E
        }
    }

    fun Registrations.domesticAbuse() = E.takeIf { hasDomesticAbuse }
    fun Registrations.stalking() = F.takeIf { hasStalking }
    fun Registrations.childProtection() = F.takeIf { hasChildProtection }
    fun OASysInputs.sexualOffences() = E.takeIf { everCommittedSexualOffence }
}
