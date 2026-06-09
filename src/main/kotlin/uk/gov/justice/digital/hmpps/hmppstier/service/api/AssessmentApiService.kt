package uk.gov.justice.digital.hmpps.hmppstier.service.api

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.ArnsApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.AssessmentForTier
import uk.gov.justice.digital.hmpps.hmppstier.domain.OASysInputs
import uk.gov.justice.digital.hmpps.hmppstier.service.RescoredAssessmentService

@Service
class AssessmentApiService(
    private val arnsApiClient: ArnsApiClient,
    private val rescoredAssessmentService: RescoredAssessmentService,
) {
    fun getTierAssessmentInformation(crn: String): AssessmentForTier? = arnsApiClient.getTierAssessmentInformation(crn)

    fun getRiskPredictors(crn: String) = arnsApiClient.getRiskPredictors(crn)
        ?.maxByOrNull { it.completedDate }

    fun getSexuallyMotivatedOffence(crn: String) = arnsApiClient.getSexuallyMotivatedOffence(crn)
        ?.everCommittedSexualOffence == true

    fun getOASysTierInputs(crn: String): OASysInputs? {
        val predictors = (getRiskPredictors(crn) ?: rescoredAssessmentService.getByCrn(crn))
        return predictors?.output?.let {
            OASysInputs(
                predictors = predictors,
                everCommittedSexualOffence = getSexuallyMotivatedOffence(crn)
            )
        }
    }
}
