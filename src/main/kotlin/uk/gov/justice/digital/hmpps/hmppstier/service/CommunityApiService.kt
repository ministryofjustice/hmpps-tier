package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusAssessments

@Service
class CommunityApiService(
  private val communityApiClient: CommunityApiClient,
) {

  public fun getDeliusAssessments(crn: String): DeliusAssessments {
    return DeliusAssessments from communityApiClient.getDeliusAssessments(crn)
  }
}
