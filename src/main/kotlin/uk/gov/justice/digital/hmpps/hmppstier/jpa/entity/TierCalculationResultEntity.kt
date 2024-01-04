package uk.gov.justice.digital.hmpps.hmppstier.jpa.entity

import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusInputs
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.*
import java.io.Serializable

data class TierCalculationResultEntity(
  val protect: TierLevel<ProtectLevel>,
  val change: TierLevel<ChangeLevel>,
  val calculationVersion: String,
  val deliusInputs: DeliusInputs? = null,
  val assessment: OffenderAssessment? = null,
  val additionalFactorsForWomen: Map<AdditionalFactorForWomen, String?>? = null,
  val needs: Map<Need, NeedSeverity>? = null,
) : Serializable
