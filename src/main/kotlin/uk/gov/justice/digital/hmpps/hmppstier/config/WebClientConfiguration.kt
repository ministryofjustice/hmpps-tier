package uk.gov.justice.digital.hmpps.hmppstier.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration(
  @Value("\${community.endpoint.url}") private val communityApiRootUri: String,
  @Value("\${assessment.endpoint.url}") private val assessmentApiRootUri: String,
  @Value("\${tier-to-delius.endpoint.url}") private val tierToDeliusApiRootUri: String,
) {

  @Bean
  fun authorizedClientManagerAppScope(
    clientRegistrationRepository: ReactiveClientRegistrationRepository,
    oAuth2AuthorizedClientService: ReactiveOAuth2AuthorizedClientService,
  ): ReactiveOAuth2AuthorizedClientManager {
    val authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager = AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
      clientRegistrationRepository,
      oAuth2AuthorizedClientService,
    )
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }

  @Bean
  fun communityWebClientAppScope(
    @Qualifier(value = "authorizedClientManagerAppScope") authorizedClientManager: ReactiveOAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient {
    return getOAuthWebClient(authorizedClientManager, builder, communityApiRootUri, "community-api")
  }

  @Bean
  fun assessmentWebClientAppScope(
    @Qualifier(value = "authorizedClientManagerAppScope") authorizedClientManager: ReactiveOAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient {
    return getOAuthWebClient(authorizedClientManager, builder, assessmentApiRootUri, "assessment-api")
  }

  @Bean
  fun tierToDeliusApiClientWebClientAppScope(
    @Qualifier(value = "authorizedClientManagerAppScope") authorizedClientManager: ReactiveOAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient {
    return getOAuthWebClient(authorizedClientManager, builder, tierToDeliusApiRootUri, "tier-to-delius-api")
  }

  private fun getOAuthWebClient(
    authorizedClientManager: ReactiveOAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    rootUri: String,
    registrationId: String,
  ): WebClient {
    val oauth2Client = ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId(registrationId)
    return builder.baseUrl(rootUri)
      .filter(oauth2Client)
      .build()
  }
}
