package uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4

import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ScoreLevel
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ScoreType
import java.math.BigDecimal

class VersionedStaticOrDynamicPredictorDto(
    val algorithmVersion: String? = null,
    staticOrDynamic: ScoreType? = null,
    score: BigDecimal? = null,
    band: ScoreLevel? = null,
) : StaticOrDynamicPredictorDto(staticOrDynamic, score, band)
