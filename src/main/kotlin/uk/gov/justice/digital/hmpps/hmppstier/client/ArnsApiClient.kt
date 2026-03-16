package uk.gov.justice.digital.hmpps.hmppstier.client

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.AllPredictorVersioned
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.AssessmentForTier
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.OGRS4Predictors

@Component
class ArnsApiClient(
    private val arnsClient: WebClient,
    private val retryOnServerError: Retry,
) {
    fun getTierAssessmentInformation(crn: String): AssessmentForTier? = arnsClient
        .get()
        .uri("/tier-assessment/sections/{crn}", crn)
        .retrieve()
        .onStatus({ it == HttpStatus.NOT_FOUND }, { Mono.empty() })
        .bodyToMono<AssessmentForTier>()
        .retryWhen(retryOnServerError)
        .block()

    fun getRiskPredictors(crn: String): List<OGRS4Predictors>? = arnsClient
        .get()
        .uri("/risks/predictors/unsafe/all/CRN/{crn}", crn)
        .retrieve()
        .onStatus({ it == HttpStatus.NOT_FOUND }, { Mono.empty() })
        .bodyToMono<List<AllPredictorVersioned<Any>>>()
        .retryWhen(retryOnServerError)
        .block()
        ?.filter { it.outputVersion == "2" && it is OGRS4Predictors }
        ?.map { it as OGRS4Predictors }
}
