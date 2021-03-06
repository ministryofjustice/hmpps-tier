package uk.gov.justice.digital.hmpps.hmppstier.integration.setup

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.time.Duration
import java.util.Date
import java.util.UUID

@Component
class JwtAuthHelper {
  private val keyPair: KeyPair

  init {
    val gen = KeyPairGenerator.getInstance("RSA")
    gen.initialize(2048)
    keyPair = gen.generateKeyPair()
  }

  @Bean
  fun jwtDecoder(): JwtDecoder = NimbusJwtDecoder.withPublicKey(keyPair.public as RSAPublicKey).build()

  fun createJwt(): String {

    val claims = mapOf(
      "authorities" to listOf("ROLE_HMPPS_TIER"),
    )

    return Jwts.builder()
      .setId(UUID.randomUUID().toString())
      .setSubject("hmpps-tier")
      .addClaims(claims)
      .setExpiration(Date(System.currentTimeMillis() + Duration.ofHours(1).toMillis()))
      .signWith(SignatureAlgorithm.RS256, keyPair.private)
      .compact()
  }
}
