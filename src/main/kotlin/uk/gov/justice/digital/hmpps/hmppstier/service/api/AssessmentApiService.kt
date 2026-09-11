package uk.gov.justice.digital.hmpps.hmppstier.service.api

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.ArnsApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.AssessmentForTier
import uk.gov.justice.digital.hmpps.hmppstier.domain.OASysInputs

@Service
class AssessmentApiService(private val arnsApiClient: ArnsApiClient) {
    fun getTierAssessmentInformation(crn: String): AssessmentForTier? = arnsApiClient.getTierAssessmentInformation(crn)

    fun getRiskPredictors(crn: String) = arnsApiClient.getTierRiskPredictors(crn)

    fun getSexuallyMotivatedOffence(crn: String) = arnsApiClient.getSexuallyMotivatedOffence(crn)
        ?.everCommittedSexualOffence == true

    fun getOASysTierInputs(crn: String): OASysInputs? {
        val predictors = getRiskPredictors(crn)
        return predictors?.output?.let {
            OASysInputs(
                predictors = predictors,
                everCommittedSexualOffence = getSexuallyMotivatedOffence(crn)
            )
        }
    }
}
