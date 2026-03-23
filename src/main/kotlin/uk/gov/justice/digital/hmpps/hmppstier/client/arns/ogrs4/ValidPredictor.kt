package uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4

import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ScoreLevel
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ScoreLevel.NOT_APPLICABLE
import java.math.BigDecimal

data class ValidPredictor(
    val score: BigDecimal,
    val band: ScoreLevel,
) {
    companion object {
        fun BasePredictorDto?.validate() = if (
            this != null &&
            score != null &&
            score > BigDecimal.ZERO &&
            band != null &&
            band != NOT_APPLICABLE
        ) ValidPredictor(score, band) else null
    }
}