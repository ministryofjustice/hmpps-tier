package uk.gov.justice.digital.hmpps.hmppstier.service.api

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.ArnsApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.AssessmentForTier

@Service
class AssessmentApiService(
    private val arnsApiClient: ArnsApiClient,
) {
    fun getTierAssessmentInformation(crn: String): AssessmentForTier? = arnsApiClient.getTierAssessmentInformation(crn)
}
