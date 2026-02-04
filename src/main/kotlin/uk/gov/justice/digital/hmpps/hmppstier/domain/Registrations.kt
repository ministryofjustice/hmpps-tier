package uk.gov.justice.digital.hmpps.hmppstier.domain

import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import java.io.Serializable

data class Registrations(
    val hasIomNominal: Boolean,
    val complexityFactors: Collection<ComplexityFactor>,
    val rosh: Rosh?,
    val mappa: Mappa?,
    val unsupervised: Boolean?
) : Serializable
