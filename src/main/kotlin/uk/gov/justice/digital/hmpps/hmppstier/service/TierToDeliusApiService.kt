package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusResponse
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusInputs
import java.math.BigDecimal

@Service
class TierToDeliusApiService(private val tierToDeliusApiClient: TierToDeliusApiClient) {

  private val mandateForChange: MandateForChange = MandateForChange()

  suspend fun getTierToDelius(crn: String): DeliusInputs {
    val tierToDeliusResponse = tierToDeliusApiClient.getDeliusTier(crn)

    return DeliusInputs(
      tierToDeliusResponse.gender.equals("female", true),
      tierToDeliusResponse.rsrscore ?: BigDecimal.ZERO,
      tierToDeliusResponse.ogrsscore ?: 0,
      tierToDeliusResponse.convictions.any { it.breached },
      mandateForChange.hasNoMandate(tierToDeliusResponse.convictions)
    )
  }
}
