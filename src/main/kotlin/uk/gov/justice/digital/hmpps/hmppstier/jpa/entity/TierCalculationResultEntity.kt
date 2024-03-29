package uk.gov.justice.digital.hmpps.hmppstier.jpa.entity

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import uk.gov.justice.digital.hmpps.hmppstier.client.AssessmentForTier
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusInputs
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import java.io.Serializable

@JsonIgnoreProperties(ignoreUnknown = true)
data class TierCalculationResultEntity(
    val protect: TierLevel<ProtectLevel>,
    val change: TierLevel<ChangeLevel>,
    val calculationVersion: String,
    val deliusInputs: DeliusInputs? = null,
    val assessmentSummary: AssessmentForTier? = null
) : Serializable
