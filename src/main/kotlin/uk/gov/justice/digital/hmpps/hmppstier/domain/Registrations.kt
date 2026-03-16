package uk.gov.justice.digital.hmpps.hmppstier.domain

import com.fasterxml.jackson.annotation.JsonAlias
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.MappaCategory
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.MappaLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import java.io.Serializable

data class Registrations(
    val hasIomNominal: Boolean,
    val hasLiferIpp: Boolean,
    val hasDomesticAbuse: Boolean,
    val hasStalking: Boolean,
    val hasChildProtection: Boolean,
    val complexityFactors: Collection<ComplexityFactor>,
    val rosh: Rosh?,
    @JsonAlias("mappa")
    val mappaLevel: MappaLevel?,
    val mappaCategory: MappaCategory?,
    val unsupervised: Boolean?
) : Serializable
