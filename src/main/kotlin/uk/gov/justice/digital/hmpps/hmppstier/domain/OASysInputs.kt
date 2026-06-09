package uk.gov.justice.digital.hmpps.hmppstier.domain

import uk.gov.justice.digital.hmpps.hmppstier.client.arns.OGRS4Predictors
import java.io.Serializable

data class OASysInputs(
    val predictors: OGRS4Predictors,
    val everCommittedSexualOffence: Boolean,
) : Serializable
