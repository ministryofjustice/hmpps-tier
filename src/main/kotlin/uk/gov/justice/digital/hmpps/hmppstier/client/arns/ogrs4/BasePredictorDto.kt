package uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4

import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ScoreLevel
import java.io.Serializable
import java.math.BigDecimal

open class BasePredictorDto(
    val score: BigDecimal? = null,
    val band: ScoreLevel? = null,
) : Serializable
