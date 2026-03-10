package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.AssessmentForTier

@Component
class ArnsApiClient(
    @Qualifier("arnsRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper
) {
    fun getTierAssessmentInformation(crn: String): AssessmentForTier? = restClient
        .get()
        .uri("/tier-assessment/sections/{crn}", crn)
        .exchange<AssessmentForTier?> { _, res ->
            when (res.statusCode) {
                HttpStatus.OK -> objectMapper.readValue(res.body)
                HttpStatus.NOT_FOUND -> null
                else -> throw HttpClientErrorException(res.statusCode, res.statusText)
            }
        }
}
