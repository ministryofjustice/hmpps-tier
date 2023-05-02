package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.domain

data class Registration(
  val registerLevel: String? = null,
  val typeCode: String = "MAPP"
)