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
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.TierDetails
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.tierDetailsResponse

class TierToDeliusApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    companion object {
        lateinit var tierToDeliusApi: TierToDeliusApiMockServer
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    override fun beforeAll(context: ExtensionContext?) {
        log.info("beforeAll called")
        tierToDeliusApi = TierToDeliusApiMockServer()
    }

    override fun beforeEach(context: ExtensionContext?) {
        log.info("beforeEach called")
        tierToDeliusApi.reset()
    }

    override fun afterAll(context: ExtensionContext?) {
        log.info("afterAll called")
        tierToDeliusApi.stop()
    }
}

class TierToDeliusApiMockServer : ClientAndServer(MOCKSERVER_PORT) {

    companion object {
        private const val MOCKSERVER_PORT = 8093
    }

    fun getFullDetails(crn: String, tierDetails: TierDetails) {
        val request = HttpRequest.request().withPath("/tier-details/$crn")
        TierToDeliusApiExtension.tierToDeliusApi.`when`(request, Times.exactly(1)).respond(
            HttpResponse.response().withContentType(MediaType.APPLICATION_JSON)
                .withBody(tierDetailsResponse(tierDetails)),
        )
    }

    fun getNotFound(crn: String) {
        val request = HttpRequest.request().withPath("/tier-details/$crn")
        TierToDeliusApiExtension.tierToDeliusApi.`when`(request, Times.exactly(1)).respond(
            HttpResponse.notFoundResponse(),
        )
    }

    fun getCrns(crns: List<String>) {
        val request = HttpRequest.request().withPath("/probation-cases")
        TierToDeliusApiExtension.tierToDeliusApi.`when`(request, Times.exactly(1)).respond(
            HttpResponse.response().withContentType(MediaType.APPLICATION_JSON)
                .withBody(ObjectMapper().writeValueAsString(crns)),
        )
    }
}
