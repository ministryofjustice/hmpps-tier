package uk.gov.justice.digital.hmpps.hmppstier.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class ClockConfiguration {

  @Bean
  fun initialiseClock(): Clock {
    return Clock.systemDefaultZone()
  }
}