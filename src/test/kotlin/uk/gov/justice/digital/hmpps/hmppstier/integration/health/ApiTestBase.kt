package uk.gov.justice.digital.hmpps.hmppstier.integration.health

import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class ApiTestBase {

    @LocalServerPort
    private var port: Int = 0

    protected lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setupClient() {
        webTestClient =
            WebTestClient.bindToServer()
                .baseUrl("http://localhost:$port")
                .build()
    }
}
