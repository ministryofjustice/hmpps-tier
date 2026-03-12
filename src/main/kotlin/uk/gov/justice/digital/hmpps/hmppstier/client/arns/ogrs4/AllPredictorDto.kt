package uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4

import java.io.Serializable

data class AllPredictorDto(
    val allReoffendingPredictor: StaticOrDynamicPredictorDto? = null,
    val violentReoffendingPredictor: StaticOrDynamicPredictorDto? = null,
    val seriousViolentReoffendingPredictor: StaticOrDynamicPredictorDto? = null,
    val directContactSexualReoffendingPredictor: BasePredictorDto? = null,
    val indirectImageContactSexualReoffendingPredictor: BasePredictorDto? = null,
    val combinedSeriousReoffendingPredictor: VersionedStaticOrDynamicPredictorDto? = null,
) : Serializable
