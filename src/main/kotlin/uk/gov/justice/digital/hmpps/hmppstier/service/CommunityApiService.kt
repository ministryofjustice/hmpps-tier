package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.domain.ConvictionWithSentence
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusAssessments

@Service
class CommunityApiService(
  private val communityApiClient: CommunityApiClient,
) {

  fun getDeliusAssessments(crn: String): DeliusAssessments =
    DeliusAssessments.from(communityApiClient.getDeliusAssessments(crn))

  fun getConvictionsWithSentences(crn: String): List<ConvictionWithSentence> {
    val convictions = communityApiClient.getConvictionsWithSentences(crn)
    return ConvictionWithSentence.from(convictions.filterNot { it.sentence == null })
  }
}
