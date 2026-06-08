package uk.gov.justice.digital.hmpps.hmppstier.domain

import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.AllPredictorDto
import java.io.Serializable

data class OASysInputs(
    val predictors: AllPredictorDto,
    val everCommittedSexualOffence: Boolean,
) : Serializable
