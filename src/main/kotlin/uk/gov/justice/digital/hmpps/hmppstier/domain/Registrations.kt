package uk.gov.justice.digital.hmpps.hmppstier.domain

import uk.gov.justice.digital.hmpps.hmppstier.client.Registration
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh

data class Registrations(
  val iomNominal: List<Registration>,
  val complexityFactors: Collection<ComplexityFactor>,
  val rosh: Rosh?,
  val mappa: Mappa?
)
