package uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4

import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ScoreLevel
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ScoreType
import java.math.BigDecimal

open class StaticOrDynamicPredictorDto(
    val staticOrDynamic: ScoreType? = null,
    score: BigDecimal? = null,
    band: ScoreLevel? = null,
) : BasePredictorDto(score, band)
