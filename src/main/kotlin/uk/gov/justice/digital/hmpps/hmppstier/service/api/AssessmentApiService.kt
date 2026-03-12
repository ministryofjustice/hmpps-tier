package uk.gov.justice.digital.hmpps.hmppstier.service.api

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.ArnsApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.AssessmentForTier
import java.time.LocalDate

@Service
class AssessmentApiService(
    private val arnsApiClient: ArnsApiClient,
) {
    fun getTierAssessmentInformation(crn: String): AssessmentForTier? = arnsApiClient.getTierAssessmentInformation(crn)
    fun getRiskPredictors(crn: String) = arnsApiClient.getRiskPredictors(crn)
        ?.filter { it.completedDate >= LocalDate.now().atStartOfDay().minusWeeks(55) }
        ?.maxByOrNull { it.completedDate }
}
