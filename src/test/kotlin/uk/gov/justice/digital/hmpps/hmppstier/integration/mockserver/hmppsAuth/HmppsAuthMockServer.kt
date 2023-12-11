package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.hmppsAuth

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.hmppsAuth.HmppsAuthApiExtension.Companion.hmppsAuth

class HmppsAuthApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    companion object {
        lateinit var hmppsAuth: HmppsAuthMockServer
    }

    override fun beforeAll(context: ExtensionContext?) {
        hmppsAuth = HmppsAuthMockServer()
    }

    override fun beforeEach(context: ExtensionContext?) {
        hmppsAuth.reset()
        hmppsAuth.setupOauth()
    }

    override fun afterAll(context: ExtensionContext?) {
        hmppsAuth.stop()
    }
}

class HmppsAuthMockServer : ClientAndServer(MOCKSERVER_PORT) {

    companion object {
        private const val MOCKSERVER_PORT = 9090
    }

    fun setupOauth() {
        val response = HttpResponse.response().withContentType(MediaType.APPLICATION_JSON)
            .withBody(
                """
        {
            "token_type": "bearer",
            "access_token": "ABCDE"
        }
        """.trimIndent(),
            )
        hmppsAuth.`when`(HttpRequest.request().withPath("/auth/oauth/token")).respond(response)
    }
}
