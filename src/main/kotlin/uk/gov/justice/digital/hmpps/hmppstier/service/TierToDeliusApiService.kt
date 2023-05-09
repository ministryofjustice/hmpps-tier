package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusConviction
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusRegistration
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusInputs
import uk.gov.justice.digital.hmpps.hmppstier.domain.Registrations
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.IomNominal
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
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
      getRegistrations(tierToDeliusResponse.registrations),
    )
  }

  fun isBreached(convictions: List<DeliusConviction>): Boolean = convictions
    .filter { qualifyingConvictions(it) }
    .any { it.breached }

  private fun qualifyingConvictions(conviction: DeliusConviction): Boolean =
    conviction.terminationDate == null ||
      conviction.terminationDate.isAfter(LocalDate.now().minusYears(1).minusDays(1))

  private fun getRegistrations(deliusRegistrations: Collection<DeliusRegistration>): Registrations {
    val registrations = deliusRegistrations
      .filter { it.code != "HREG" }
      .sortedByDescending { it.date }
    return Registrations(
      hasIomNominal(registrations),
      getComplexityFactors(registrations),
      getRosh(registrations),
      getMappa(registrations),
    )
  }
  private fun getRosh(registrations: Collection<DeliusRegistration>): Rosh? =
    registrations.mapNotNull { Rosh.from(it.code) }.firstOrNull()

  private fun getMappa(registrations: Collection<DeliusRegistration>): Mappa? =
    registrations.mapNotNull { Mappa.from(it.level, it.code) }.firstOrNull()

  private fun getComplexityFactors(registrations: Collection<DeliusRegistration>): Collection<ComplexityFactor> =
    registrations.mapNotNull { ComplexityFactor.from(it.code) }.distinct()

  private fun hasIomNominal(registrations: Collection<DeliusRegistration>): Boolean =
    registrations.any { it.code == IomNominal.IOM_NOMINAL.registerCode }
}
