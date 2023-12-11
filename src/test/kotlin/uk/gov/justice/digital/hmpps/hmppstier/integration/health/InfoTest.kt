package uk.gov.justice.digital.hmpps.hmppstier.integration.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class InfoTest : ApiTestBase() {

    @Test
    fun `Info page is accessible`() {
        webTestClient.get()
            .uri("/info")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("build.name").isEqualTo("hmpps-tier")
    }

    @Test
    fun `Info page reports version`() {
        webTestClient.get().uri("/info")
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("build.version").value<String> {
                assertThat(it).startsWith(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
            }
    }
}
