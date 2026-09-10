package uk.gov.justice.digital.hmpps.hmppstier.service.api

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusConviction
import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusRegistration
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusInputs
import uk.gov.justice.digital.hmpps.hmppstier.domain.Registrations
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.*
import uk.gov.justice.digital.hmpps.hmppstier.service.calculation.MandateForChange.hasNoMandate
import java.math.BigDecimal

@Service
class DeliusApiService(private val deliusApiClient: DeliusApiClient) {
    fun getTierToDelius(crn: String): DeliusInputs {
        val tierToDeliusResponse = deliusApiClient.getDeliusTierInputs(crn)

        return DeliusInputs(
            isFemale = tierToDeliusResponse.gender.equals("female", true),
            rsrScore = tierToDeliusResponse.rsrscore ?: BigDecimal.ZERO,
            ogrsScore = tierToDeliusResponse.ogrsscore ?: 0,
            hasNoMandate = tierToDeliusResponse.convictions.hasNoMandate(),
            registrations = getRegistrations(tierToDeliusResponse.registrations),
            previousEnforcementActivity = tierToDeliusResponse.previousEnforcementActivity,
            latestReleaseDate = tierToDeliusResponse.latestReleaseDate,
            hasActiveEvent = tierToDeliusResponse.hasActiveEvent,
            latestSentencingAct2026ExcludedOffenceDate = tierToDeliusResponse.activeConvictions
                .filter { it.offenceIsExcludedFromSentencingAct2026() }
                .latestSupervisionStartDate,
            latestChildSexualExploitationSentenceDate = if (hasChildSexualExploitation(tierToDeliusResponse.registrations)) {
                tierToDeliusResponse.activeConvictions.latestSupervisionStartDate
            } else null
        )
    }

    val List<DeliusConviction>.latestSupervisionStartDate
        get() = mapNotNull { if (it.isCustodial) it.latestReleaseDate else it.startDate }.maxOrNull()

    private fun getRegistrations(deliusRegistrations: Collection<DeliusRegistration>): Registrations {
        val registrations = deliusRegistrations
            .filter { it.code != "HREG" }
            .sortedByDescending { it.date }
        return Registrations(
            hasIomNominal = hasIomNominal(registrations),
            hasLiferIpp = hasLiferIpp(registrations),
            hasDomesticAbuse = hasDomesticAbuse(registrations),
            hasStalking = hasStalking(registrations),
            hasChildProtection = hasChildProtection(registrations),
            complexityFactors = getComplexityFactors(registrations),
            rosh = getRosh(registrations),
            mappaLevel = getMappaLevel(registrations),
            mappaCategory = getMappaCategory(registrations),
            unsupervised = isUnsupervised(registrations),
        )
    }

    private fun getRosh(registrations: Collection<DeliusRegistration>): Rosh? =
        registrations.firstNotNullOfOrNull { Rosh.from(it.code) }

    private fun getMappaLevel(registrations: Collection<DeliusRegistration>): MappaLevel? =
        registrations.firstNotNullOfOrNull { MappaLevel.from(it.level, it.code) }

    private fun getMappaCategory(registrations: Collection<DeliusRegistration>): MappaCategory? =
        registrations.firstOrNull { it.code == DeliusRegistration.MAPPA }?.category?.let { MappaCategory.from(it) }

    private fun getComplexityFactors(registrations: Collection<DeliusRegistration>): Collection<ComplexityFactor> =
        registrations.mapNotNull { ComplexityFactor.from(it.code) }.distinct()

    private fun hasIomNominal(registrations: Collection<DeliusRegistration>): Boolean =
        registrations.any { it.code == IomNominal.IOM_NOMINAL.registerCode }

    private fun hasLiferIpp(registrations: Collection<DeliusRegistration>): Boolean =
        registrations.any { it.code == DeliusRegistration.LIFER }

    private fun hasDomesticAbuse(registrations: Collection<DeliusRegistration>): Boolean =
        registrations.any { it.code == DeliusRegistration.DOMESTIC_ABUSE || it.code == DeliusRegistration.DOMESTIC_ABUSE_HISTORY }

    private fun hasStalking(registrations: Collection<DeliusRegistration>): Boolean =
        registrations.any { it.code == DeliusRegistration.STALKING }

    private fun hasChildProtection(registrations: Collection<DeliusRegistration>): Boolean =
        registrations.any { it.code == DeliusRegistration.CHILD_PROTECTION }

    private fun hasChildSexualExploitation(registrations: Collection<DeliusRegistration>): Boolean =
        registrations.any { it.code == DeliusRegistration.CHILD_SEXUAL_EXPLOITATION }

    private fun isUnsupervised(registrations: Collection<DeliusRegistration>): Boolean =
        registrations.any { it.code == DeliusRegistration.TWO_THIRDS_CODE }

    private fun DeliusConviction.offenceIsExcludedFromSentencingAct2026(): Boolean =
        mainOffence.sentencingAct2026Exclusion || additionalOffences.any { it.sentencingAct2026Exclusion }
}