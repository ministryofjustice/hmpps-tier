package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.objectMapper

class TierToDeliusApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    companion object {
        lateinit var deliusApi: TierToDeliusApiMockServer
    }

    override fun beforeAll(context: ExtensionContext) = start()

    override fun beforeEach(context: ExtensionContext) = reset()

    override fun afterAll(context: ExtensionContext) = stop()

    fun start() {
        deliusApi = TierToDeliusApiMockServer()
    }

    fun reset() {
        deliusApi.reset()
    }

    fun stop() {
        deliusApi.stop()
    }
}

class TierToDeliusApiMockServer : ClientAndServer(MOCKSERVER_PORT) {

    companion object {
        private const val MOCKSERVER_PORT = 8093
    }

    fun getFullDetails(crn: String, response: DeliusResponse) {
        val request = HttpRequest.request().withPath("/tier-details/$crn")
        TierToDeliusApiExtension.deliusApi.`when`(request, Times.exactly(1)).respond(
            HttpResponse.response().withContentType(MediaType.APPLICATION_JSON)
                .withBody(objectMapper().writeValueAsString(response)),
        )
    }

    fun getNotFound(crn: String) {
        val request = HttpRequest.request().withPath("/tier-details/$crn")
        TierToDeliusApiExtension.deliusApi.`when`(request, Times.exactly(1)).respond(
            HttpResponse.notFoundResponse(),
        )
    }

    fun getCrns(crns: List<String>) {
        val request = HttpRequest.request().withPath("/probation-cases")
        TierToDeliusApiExtension.deliusApi.`when`(request, Times.exactly(1)).respond(
            HttpResponse.response().withContentType(MediaType.APPLICATION_JSON)
                .withBody(ObjectMapper().writeValueAsString(crns)),
        )
    }
}
