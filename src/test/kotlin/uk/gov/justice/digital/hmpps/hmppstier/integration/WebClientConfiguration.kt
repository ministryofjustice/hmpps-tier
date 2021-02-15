package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@Profile("test")
class WebClientConfiguration(
  @Value("\${community.endpoint.url}") private val communityApiRootUri: String,
  @Value("\${assessment.endpoint.url}") private val assessmentApiRootUri: String
) {

  @Bean
  fun communityWebClientAppScope(): WebClient {
    return WebClient.builder().baseUrl(communityApiRootUri).build()
  }

  @Bean
  fun assessmentWebClientAppScope(): WebClient {
    return WebClient.builder().baseUrl(assessmentApiRootUri).build()
  }
}
