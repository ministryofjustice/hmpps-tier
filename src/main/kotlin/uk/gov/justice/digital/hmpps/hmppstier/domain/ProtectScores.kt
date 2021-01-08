package uk.gov.justice.digital.hmpps.hmppstier.domain

import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AssessmentComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh


data class ProtectScores(
  val crn: String,
  val mappaLevel: Mappa?,
  val rsrScore: Int?,
  val roshScore: Rosh?,
  val complexityFactors: List<ComplexityFactor>,
  val assessmentComplexityFactors: List<AssessmentComplexityFactor>
)