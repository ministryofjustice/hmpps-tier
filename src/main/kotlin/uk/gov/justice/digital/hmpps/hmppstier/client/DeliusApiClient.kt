package uk.gov.justice.digital.hmpps.hmppstier.client

import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.util.retry.Retry
import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusResponse
import uk.gov.justice.digital.hmpps.hmppstier.exception.CrnNotFoundException

@Component
class DeliusApiClient(
    private val deliusClient: WebClient,
    private val retryOnServerError: Retry,
) {
    fun getDeliusTierInputs(crn: String): DeliusResponse = deliusClient
        .get()
        .uri("/tier-details/{crn}", crn)
        .retrieve()
        .bodyToMono<DeliusResponse>()
        .onErrorMap(WebClientResponseException.NotFound::class.java) { e -> CrnNotFoundException(crn, e) }
        .retryWhen(retryOnServerError)
        .block()!!

    fun getActiveCrns(): List<String> = deliusClient
        .get()
        .uri("/probation-cases")
        .accept(APPLICATION_JSON)
        .retrieve()
        .bodyToMono<List<String>>()
        .retryWhen(retryOnServerError)
        .block()!!
}

