package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusConviction
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusInputs
import java.math.BigDecimal
import java.time.LocalDate

@Service
class TierToDeliusApiService(private val tierToDeliusApiClient: TierToDeliusApiClient) {

  private val mandateForChange: MandateForChange = MandateForChange()

  suspend fun getTierToDelius(crn: String): DeliusInputs {
    val tierToDeliusResponse = tierToDeliusApiClient.getDeliusTier(crn)

    return DeliusInputs(
      tierToDeliusResponse.gender.equals("female", true),
      tierToDeliusResponse.rsrscore ?: BigDecimal.ZERO,
      tierToDeliusResponse.ogrsscore ?: 0,
      isBreached(tierToDeliusResponse.convictions),
      mandateForChange.hasNoMandate(tierToDeliusResponse.convictions),
    )
  }

  fun isBreached(convictions: List<DeliusConviction>): Boolean = convictions
    .filter { qualifyingConvictions(it) }
    .any { it.breached }

  private fun qualifyingConvictions(conviction: DeliusConviction): Boolean =
    conviction.terminationDate == null ||
      conviction.terminationDate.isAfter(LocalDate.now().minusYears(1).minusDays(1))
}
