package uk.gov.justice.digital.hmpps.hmppstier.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class RestClientBuilderConfiguration {

    @Bean
    fun restClientBuilder(): RestClient.Builder =
        RestClient.builder()
}
