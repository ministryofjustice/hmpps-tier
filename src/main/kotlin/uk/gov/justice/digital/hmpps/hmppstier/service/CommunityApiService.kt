package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.Registration
import uk.gov.justice.digital.hmpps.hmppstier.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusAssessments
import uk.gov.justice.digital.hmpps.hmppstier.domain.Registrations
import uk.gov.justice.digital.hmpps.hmppstier.domain.Requirement
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.IomNominal.IOM_NOMINAL
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NsiOutcome
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh

@Service
class CommunityApiService(
  private val communityApiClient: CommunityApiClient,
) {

  suspend fun getDeliusAssessments(crn: String): DeliusAssessments =
    DeliusAssessments.from(communityApiClient.getDeliusAssessments(crn))

  suspend fun getConvictionsWithSentences(crn: String): List<Conviction> =
    communityApiClient.getConvictions(crn).filterNot { it.sentence == null }.map { Conviction.from(it) }

  suspend fun getRegistrations(crn: String): Registrations {
    val registrations = communityApiClient.getRegistrations(crn)
      .filter { registration -> registration.type.code != "HREG" }
      .sortedByDescending { it.startDate }
    return Registrations(
      hasIomNominal(registrations),
      getComplexityFactors(registrations),
      getRosh(registrations),
      getMappa(registrations)
    )
  }

  suspend fun getRequirements(crn: String, convictionId: Long): List<Requirement> {
    return communityApiClient.getRequirements(crn, convictionId)
      .filterNot { it.requirementTypeMainCategory == null && it.restrictive == null }
      .map { Requirement(it.restrictive!!, it.requirementTypeMainCategory!!.code) }
  }

  suspend fun hasBreachedConvictions(crn: String, convictions: List<Conviction>): Boolean =
    convictions.any { hasBreachOrRecallNsis(crn, it.convictionId) }

  suspend fun offenderIsFemale(crn: String): Boolean = getOffender(crn)?.gender.equals("female", true)

  private suspend fun getOffender(crn: String) = communityApiClient.getOffender(crn)

  private fun getRosh(registrations: Collection<Registration>): Rosh? =
    registrations.mapNotNull { Rosh.from(it.type.code) }.firstOrNull()

  private fun getMappa(registrations: Collection<Registration>): Mappa? =
    registrations.mapNotNull { Mappa.from(it.registerLevel?.code, it.type.code) }.firstOrNull()

  private fun getComplexityFactors(registrations: Collection<Registration>): Collection<ComplexityFactor> =
    registrations.mapNotNull { ComplexityFactor.from(it.type.code) }.distinct()

  private fun hasIomNominal(registrations: Collection<Registration>): Boolean =
    registrations.any { it.type.code == IOM_NOMINAL.registerCode }

  private suspend fun hasBreachOrRecallNsis(crn: String, convictionId: Long): Boolean =
    communityApiClient.getBreachRecallNsis(crn, convictionId)
      .any { NsiOutcome.from(it.status?.code) != null }
}
