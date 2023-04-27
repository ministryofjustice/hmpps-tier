package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusResponse
import java.math.BigDecimal

@Service
class TierToDeliusApiService(private val tierToDeliusApiClient: TierToDeliusApiClient) {

  suspend fun getTierToDelius(crn: String): TierToDeliusResponse {
    val tierToDeliusResponse = tierToDeliusApiClient.getDeliusTier(crn)

    return TierToDeliusResponse(
      tierToDeliusResponse.gender,
      tierToDeliusResponse.currentTier,
      tierToDeliusResponse.registrations,
      tierToDeliusResponse.convictions,
      tierToDeliusResponse.rsrscore ?: BigDecimal.ZERO,
      tierToDeliusResponse.ogrsscore ?: 0,
    )
  }
}
