package uk.gov.justice.digital.hmpps.hmppstier.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.DefaultSecurityFilterChain
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.AntPathRequestMatcher

@Configuration
@EnableWebSecurity
class ResourceServerConfiguration {
  @Bean
  fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
    return http
      .authorizeHttpRequests {
        it.requestMatchers(
          AntPathRequestMatcher("/webjars/**"),
          AntPathRequestMatcher("/favicon.ico"),
          AntPathRequestMatcher("/health/**"),
          AntPathRequestMatcher("/info"),
          AntPathRequestMatcher("/h2-console/**"),
          AntPathRequestMatcher("/v3/api-docs/**"),
          AntPathRequestMatcher("/swagger-ui/**"),
          AntPathRequestMatcher("/swagger-ui.html"),
          AntPathRequestMatcher("/queue-admin/retry-all-dlqs"),
        ).permitAll().anyRequest().authenticated()
      }
      .csrf { it.disable() }
      .oauth2ResourceServer {
        it.jwt { jwt -> jwt.jwtAuthenticationConverter(AuthAwareTokenConverter()) }
      }.build()
  }
}
