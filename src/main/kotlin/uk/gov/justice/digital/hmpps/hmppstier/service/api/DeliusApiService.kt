package uk.gov.justice.digital.hmpps.hmppstier.service.api

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusRegistration
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusInputs
import uk.gov.justice.digital.hmpps.hmppstier.domain.Registrations
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.IomNominal
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.service.calculation.MandateForChange.hasNoMandate
import java.math.BigDecimal

@Service
class DeliusApiService(private val deliusApiClient: DeliusApiClient) {
    fun getTierToDelius(crn: String): DeliusInputs {
        val tierToDeliusResponse = deliusApiClient.getDeliusTier(crn)

        return DeliusInputs(
            isFemale = tierToDeliusResponse.gender.equals("female", true),
            rsrScore = tierToDeliusResponse.rsrscore ?: BigDecimal.ZERO,
            ogrsScore = tierToDeliusResponse.ogrsscore ?: 0,
            hasNoMandate = tierToDeliusResponse.convictions.hasNoMandate(),
            registrations = getRegistrations(tierToDeliusResponse.registrations),
            previousEnforcementActivity = tierToDeliusResponse.previousEnforcementActivity,
        )
    }

    private fun getRegistrations(deliusRegistrations: Collection<DeliusRegistration>): Registrations {
        val registrations = deliusRegistrations
            .filter { it.code != "HREG" }
            .sortedByDescending { it.date }
        return Registrations(
            hasIomNominal = hasIomNominal(registrations),
            complexityFactors = getComplexityFactors(registrations),
            rosh = getRosh(registrations),
            mappa = getMappa(registrations),
            unsupervised = isUnsupervised(registrations)
        )
    }

    private fun getRosh(registrations: Collection<DeliusRegistration>): Rosh? =
        registrations.firstNotNullOfOrNull { Rosh.from(it.code) }

    private fun getMappa(registrations: Collection<DeliusRegistration>): Mappa? =
        registrations.firstNotNullOfOrNull { Mappa.from(it.level, it.code) }

    private fun getComplexityFactors(registrations: Collection<DeliusRegistration>): Collection<ComplexityFactor> =
        registrations.mapNotNull { ComplexityFactor.from(it.code) }.distinct()

    private fun hasIomNominal(registrations: Collection<DeliusRegistration>): Boolean =
        registrations.any { it.code == IomNominal.IOM_NOMINAL.registerCode }

    private fun isUnsupervised(registrations: Collection<DeliusRegistration>): Boolean =
        registrations.any { it.code == DeliusRegistration.TWO_THIRDS_CODE }
}
