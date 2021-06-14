package uk.gov.justice.digital.hmpps.hmppstier.domain

data class Requirement constructor(
  val isRestrictive: Boolean,
  val mainCategory: String
)
