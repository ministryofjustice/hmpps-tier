package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain

data class TierDetails(
  val gender: String,
  val currentTier: String?,
  val ogrsScore: String?,
  val rsrScore: String?,
)
