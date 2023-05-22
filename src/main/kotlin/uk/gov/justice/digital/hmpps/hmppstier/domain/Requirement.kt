package uk.gov.justice.digital.hmpps.hmppstier.domain

import uk.gov.justice.digital.hmpps.hmppstier.config.Generated

@Generated
data class Requirement constructor(
  val isRestrictive: Boolean,
  val mainCategory: String,
)
