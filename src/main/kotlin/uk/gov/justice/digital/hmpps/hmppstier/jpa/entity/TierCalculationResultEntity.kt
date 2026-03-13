package uk.gov.justice.digital.hmpps.hmppstier.jpa.entity

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.AssessmentForTier
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.OGRS4Predictors
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusInputs
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier
import java.io.Serializable

@JsonIgnoreProperties(ignoreUnknown = true)
data class TierCalculationResultEntity(
    val tier: Tier? = null,
    @Deprecated("Single tier value provided as of calculation version 3", ReplaceWith("tier"))
    val protect: TierLevel<ProtectLevel>,
    @Deprecated("Single tier value provided as of calculation version 3", ReplaceWith("tier"))
    val change: TierLevel<ChangeLevel>,
    val calculationVersion: String,
    val deliusInputs: DeliusInputs? = null,
    val assessmentSummary: AssessmentForTier? = null,
    val riskPredictors: OGRS4Predictors? = null,
) : Serializable
