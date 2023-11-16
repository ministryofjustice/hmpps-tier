package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain

data class TierDetails(
  val gender: String = "Male",
  val currentTier: String? = "UDO",
  val ogrsScore: String? = "21",
  val rsrScore: String? = "23",
  val convictions: List<Conviction> = listOf(),
  val registrations: List<Registration> = listOf(),
  val previousEnforcementActivity: Boolean = false,
)
