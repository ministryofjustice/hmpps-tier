package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusResponse
import uk.gov.justice.digital.hmpps.hmppstier.exception.CrnNotFoundException

@Component
class DeliusApiClient(
    @Qualifier("tierToDeliusRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
) {
    fun getDeliusTier(crn: String): DeliusResponse {
        return restClient
            .get()
            .uri("/tier-details/{crn}", crn)
            .exchange { req, res ->
                when (res.statusCode) {
                    HttpStatus.OK -> objectMapper.readValue<DeliusResponse>(res.body)
                    HttpStatus.NOT_FOUND -> throw CrnNotFoundException("Not Found from GET ${req.uri}")

                    else -> throw HttpClientErrorException(res.statusCode, res.statusText)
                }
            }
    }

    fun getActiveCrns(): List<String> = restClient
        .get()
        .uri("/probation-cases")
        .accept(APPLICATION_JSON)
        .retrieve()
        .body<List<String>>()!!
}

