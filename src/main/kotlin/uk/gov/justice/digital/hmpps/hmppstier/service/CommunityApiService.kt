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

  fun getDeliusAssessments(crn: String): DeliusAssessments =
    DeliusAssessments.from(communityApiClient.getDeliusAssessments(crn))

  fun getConvictionsWithSentences(crn: String): List<Conviction> =
    communityApiClient.getConvictions(crn).filterNot { it.sentence == null }.map { Conviction.from(it) }

  fun getRegistrations(crn: String): Registrations {
    val registrations = communityApiClient.getRegistrations(crn).sortedByDescending { it.startDate }
    return Registrations(
      hasIomNominal(registrations),
      getComplexityFactors(registrations),
      getRosh(registrations),
      getMappa(registrations)
    )
  }

  fun getRequirements(crn: String, convictionId: Long): List<Requirement> {
    return communityApiClient.getRequirements(crn, convictionId)
      .filterNot { it.requirementTypeMainCategory == null && it.restrictive == null }
      .map { Requirement(it.restrictive!!, it.requirementTypeMainCategory!!.code) }
  }

  fun hasBreachedConvictions(crn: String, convictions: List<Conviction>): Boolean =
    convictions.any { convictionHasBreachOrRecallNsis(crn, it.convictionId) }

  fun offenderIsFemale(crn: String): Boolean = communityApiClient.getOffender(crn)?.gender.equals("female", true)

  private fun getRosh(registrations: Collection<Registration>): Rosh? =
    registrations.mapNotNull { Rosh.from(it.type.code) }.firstOrNull()

  private fun getMappa(registrations: Collection<Registration>): Mappa? =
    registrations.mapNotNull { Mappa.from(it.registerLevel?.code) }.firstOrNull()

  private fun getComplexityFactors(registrations: Collection<Registration>): Collection<ComplexityFactor> =
    registrations.mapNotNull { ComplexityFactor.from(it.type.code) }.distinct()

  private fun hasIomNominal(registrations: Collection<Registration>): Boolean =
    registrations.any { it.type.code == IOM_NOMINAL.registerCode }

  private fun convictionHasBreachOrRecallNsis(crn: String, convictionId: Long): Boolean =
    communityApiClient.getBreachRecallNsis(crn, convictionId)
      .any { NsiOutcome.from(it.status?.code) != null }
}
