package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.domain

data class Requirement(
  val mainTypeCode: String? = null,
  val subTypeCode: String? = null,
  val restrictive: Boolean? = null,
  val length: Int? = 20,
  val lengthUnit: String? = "Days",
  val additionalMainTypeCode: String? = null,
  val additionalSubTypeCode: String? = null,
)
