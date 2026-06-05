package uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4

import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ScoreLevel.NOT_APPLICABLE
import java.math.BigDecimal

data class ValidPredictor(
    val score: BigDecimal,
    val band: ValidScoreLevel,
) {
    enum class ValidScoreLevel(val type: String) {
        LOW("Low"),
        MEDIUM("Medium"),
        HIGH("High"),
        VERY_HIGH("Very High"),
    }

    companion object {
        fun BasePredictorDto?.validate() = if (
            this != null &&
            score != null &&
            score > BigDecimal.ZERO &&
            band != null &&
            band != NOT_APPLICABLE
        ) ValidPredictor(score, ValidScoreLevel.valueOf(band.toString())) else null
    }
}