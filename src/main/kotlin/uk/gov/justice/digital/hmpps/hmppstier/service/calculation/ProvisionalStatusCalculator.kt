package uk.gov.justice.digital.hmpps.hmppstier.service.calculation

import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ScoreType
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.AllPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.StaticOrDynamicPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusInputs
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier.A
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier.G
import uk.gov.justice.digital.hmpps.hmppstier.service.calculation.CalculationStep.REOFFENDING
import uk.gov.justice.digital.hmpps.hmppstier.service.calculation.CalculationStep.SEXUAL_REOFFENDING
import java.math.BigDecimal
import java.math.BigDecimal.ZERO

internal object ProvisionalStatusCalculator {
    fun isProvisional(
        deliusInputs: DeliusInputs,
        riskPredictors: AllPredictorDto,
        stepResults: Map<CalculationStep, Tier?>,
    ) = provisionalBecausePredictorsAreStatic(riskPredictors, stepResults)
        || provisionalBecauseRoshIsMissing(deliusInputs, riskPredictors, stepResults)

    private fun provisionalBecausePredictorsAreStatic(
        riskPredictors: AllPredictorDto,
        stepResults: Map<CalculationStep, Tier?>,
    ) = with(riskPredictors) {
        val arpStatic = allReoffendingPredictor.isStatic()
        val csrpStatic = combinedSeriousReoffendingPredictor.isStatic()
        val tierWithoutArpOrCsrp = stepResults.filter { it.key != REOFFENDING }.mapNotNull { it.value }.maxOrNull() ?: G
        val maxPossibleTierForStaticArp = ReoffendingPredictorTable.calculate(
            arp = MAX_VALUE,
            csrp = combinedSeriousReoffendingPredictor?.score ?: ZERO
        )
        val maxPossibleTierForStaticCsrp = ReoffendingPredictorTable.calculate(
            arp = allReoffendingPredictor?.score ?: ZERO,
            csrp = MAX_VALUE,
        )

        when {
            arpStatic && csrpStatic -> tierWithoutArpOrCsrp < A
            arpStatic -> tierWithoutArpOrCsrp < maxPossibleTierForStaticArp
            csrpStatic -> tierWithoutArpOrCsrp < maxPossibleTierForStaticCsrp
            else -> false
        }
    }

    private fun provisionalBecauseRoshIsMissing(
        deliusInputs: DeliusInputs,
        riskPredictors: AllPredictorDto,
        stepResults: Map<CalculationStep, Tier?>,
    ) = deliusInputs.registrations.rosh == null &&
        (stepResults[REOFFENDING] != A || !riskPredictors.hasDynamicArpAndCsrp()) &&
        stepResults[SEXUAL_REOFFENDING] != A

    private fun StaticOrDynamicPredictorDto?.isStatic() = this?.staticOrDynamic == ScoreType.STATIC
    private fun StaticOrDynamicPredictorDto?.isDynamic() = this?.staticOrDynamic == ScoreType.DYNAMIC
    private fun AllPredictorDto.hasDynamicArpAndCsrp() =
        allReoffendingPredictor.isDynamic() && combinedSeriousReoffendingPredictor.isDynamic()

    private val MAX_VALUE = BigDecimal.valueOf(Long.MAX_VALUE)
}
