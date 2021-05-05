package uk.gov.justice.digital.hmpps.hmppstier.integration.setup

import org.mockserver.integration.ClientAndServer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("cucumber")
class ApiConfiguration {

  @Bean
  fun oauthMock(): ClientAndServer = ClientAndServer.startClientAndServer(9090)

  @Bean
  fun communityApi(): ClientAndServer = ClientAndServer.startClientAndServer(8091)

  @Bean
  fun assessmentApi(): ClientAndServer = ClientAndServer.startClientAndServer(8092)
}
