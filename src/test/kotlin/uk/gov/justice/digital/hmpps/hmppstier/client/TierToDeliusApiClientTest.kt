package uk.gov.justice.digital.hmpps.hmppstier.client

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase

class TierToDeliusApiClientTest : IntegrationTestBase() {
  @Test
  fun `can get tier to delius response`() {
    val crn = "X123456"
    setupTierToDelius(crn)

    webTestClient.get()
      .uri("/tier-details-test/$crn")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.gender")
      .isEqualTo("Male")
      .jsonPath("$.currentTier")
      .isEqualTo("UD0")
      .jsonPath("$.rsrscore")
      .isEqualTo(23.0)
      .jsonPath("$.ogrsscore")
      .isEqualTo(21)
  }
}
