package uk.gov.justice.digital.hmpps.hmppstier.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.security.oauth2.client.*
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Configuration
class RestClientConfiguration(
    @Value("\${arns.endpoint.url}") private val arnsApiRootUri: String,
    @Value("\${tier-to-delius.endpoint.url}") private val tierToDeliusApiRootUri: String,
) {

    @Bean
    fun authorizedClientManager(
        clientRegistration: ClientRegistrationRepository,
    ): OAuth2AuthorizedClientManager {
        val service = InMemoryOAuth2AuthorizedClientService(clientRegistration)
        val authorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistration, service)

        val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder
            .builder()
            .clientCredentials()
            .build()
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
        return authorizedClientManager
    }

    @Bean
    fun arnsRestClient(
        authorizedClientManager: OAuth2AuthorizedClientManager,
        builder: RestClient.Builder,
    ): RestClient {
        return getOAuthWebClient(authorizedClientManager, builder, arnsApiRootUri, "assessment-api")
    }

    @Bean
    fun tierToDeliusRestClient(
        authorizedClientManager: OAuth2AuthorizedClientManager,
        builder: RestClient.Builder,
    ): RestClient {
        return getOAuthWebClient(authorizedClientManager, builder, tierToDeliusApiRootUri, "tier-to-delius-api")
    }

    private fun getOAuthWebClient(
        clientManager: OAuth2AuthorizedClientManager,
        builder: RestClient.Builder,
        rootUri: String,
        registrationId: String,
    ) = builder
        .requestFactory(withTimeouts(Duration.ofSeconds(1), Duration.ofSeconds(40)))
        .requestInterceptor(HmppsAuthInterceptor(clientManager, registrationId))
        .requestInterceptor(RetryInterceptor())
        .baseUrl(rootUri)
        .defaultHeaders {
            it.contentType = MediaType.APPLICATION_JSON
            it.accept = listOf(MediaType.APPLICATION_JSON)
        }
        .build()

    fun withTimeouts(connection: Duration, read: Duration) =
        JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(connection).build())
            .also { it.setReadTimeout(read) }
}
