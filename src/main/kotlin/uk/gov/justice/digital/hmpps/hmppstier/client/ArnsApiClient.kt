package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.AllPredictorVersioned
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.AssessmentForTier
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.OGRS4Predictors

@Component
class ArnsApiClient(
    @Qualifier("arnsRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper
) {
    fun getTierAssessmentInformation(crn: String): AssessmentForTier? = restClient
        .get()
        .uri("/tier-assessment/sections/{crn}", crn)
        .exchange { _, res ->
            when (res.statusCode) {
                HttpStatus.OK -> objectMapper.readValue(res.body)
                HttpStatus.NOT_FOUND -> null
                else -> throw HttpClientErrorException(res.statusCode, res.statusText)
            }
        }

    fun getRiskPredictors(crn: String): List<OGRS4Predictors>? = restClient
        .get()
        .uri("/risks/predictors/unsafe/all/CRN/{crn}", crn)
        .exchange<List<AllPredictorVersioned<Any>>?> { _, res ->
            when (res.statusCode) {
                HttpStatus.OK -> objectMapper.readValue(res.body)
                HttpStatus.NOT_FOUND -> null
                else -> throw HttpClientErrorException(res.statusCode, res.statusText)
            }
        }
        ?.filter { it.outputVersion == "2" && it is OGRS4Predictors }
        ?.map { it as OGRS4Predictors }
}
