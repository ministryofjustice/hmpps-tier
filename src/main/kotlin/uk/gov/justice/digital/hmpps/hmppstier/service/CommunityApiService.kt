package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.config.Generated
import uk.gov.justice.digital.hmpps.hmppstier.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusAssessments
import uk.gov.justice.digital.hmpps.hmppstier.domain.Requirement
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NsiOutcome

@Service
@Generated
class CommunityApiService(
  private val communityApiClient: CommunityApiClient,
) {

  suspend fun getConvictionsWithSentences(crn: String): List<Conviction> =
    communityApiClient.getConvictions(crn).filterNot { it.sentence == null }.map { Conviction.from(it) }

  suspend fun getRequirements(crn: String, convictionId: Long): List<Requirement> {
    return communityApiClient.getRequirements(crn, convictionId)
      .filterNot { it.requirementTypeMainCategory == null && it.restrictive == null }
      .map { Requirement(it.restrictive!!, it.requirementTypeMainCategory!!.code) }
  }

  suspend fun hasBreachedConvictions(crn: String, convictions: List<Conviction>): Boolean =
    convictions.any { hasBreachOrRecallNsis(crn, it.convictionId) }

  suspend fun getOffender(crn: String) = communityApiClient.getOffender(crn)

  private suspend fun hasBreachOrRecallNsis(crn: String, convictionId: Long): Boolean =
    communityApiClient.getBreachRecallNsis(crn, convictionId)
      .any { NsiOutcome.from(it.status?.code) != null }

  suspend fun getDeliusAssessments(crn: String): DeliusAssessments =
    DeliusAssessments.from(communityApiClient.getDeliusAssessments(crn))
}
