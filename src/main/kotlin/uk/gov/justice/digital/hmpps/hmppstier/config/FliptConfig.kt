package uk.gov.justice.digital.hmpps.hmppstier.config

import io.flipt.client.FliptClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty("flipt.url")
class FliptConfig(@Value("\${flipt.url}") private val url: String) {
    @Bean
    fun fliptApiClient(): FliptClient = FliptClient.builder().namespace("probation-integration").url(url).build()
}
