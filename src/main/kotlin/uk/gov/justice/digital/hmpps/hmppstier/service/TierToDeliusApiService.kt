package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusResponse

@Service
class TierToDeliusApiService(private val tierToDeliusApiClient: TierToDeliusApiClient) {

  suspend fun getTierToDelius(crn: String): TierToDeliusResponse {
    return tierToDeliusApiClient.getDeliusTier(crn)
  }
}
