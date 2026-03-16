package uk.gov.justice.digital.hmpps.hmppstier.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.util.retry.Retry
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.oAuth2AuthorizedClientProvider
import java.time.Duration

@Configuration
class RestClientConfiguration(
    @Value("\${arns.endpoint.url}") private val arnsApiRootUri: String,
    @Value("\${tier-to-delius.endpoint.url}") private val tierToDeliusApiRootUri: String,
    @Value("\${auth.timeout:2s}") private val authTimeout: Duration,
) {
    @Bean
    fun authorizedClientProvider(): OAuth2AuthorizedClientProvider = oAuth2AuthorizedClientProvider(authTimeout)

    @Bean
    fun arnsClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder) =
        builder.authorisedWebClient(authorizedClientManager, "assessment-api", arnsApiRootUri)

    @Bean
    fun deliusClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder) =
        builder.authorisedWebClient(authorizedClientManager, "tier-to-delius-api", tierToDeliusApiRootUri)

    @Bean
    fun retryOnServerError(): Retry = Retry.backoff(3, Duration.ofMillis(100))
        .filter { error -> error !is WebClientResponseException || error.statusCode.is5xxServerError }
}
