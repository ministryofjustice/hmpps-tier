package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.arnsApi

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
import uk.gov.justice.digital.hmpps.hmppstier.client.AssessedNeed
import uk.gov.justice.digital.hmpps.hmppstier.client.AssessedNeeds
import uk.gov.justice.digital.hmpps.hmppstier.client.AssessmentSummary
import uk.gov.justice.digital.hmpps.hmppstier.client.Timeline
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.arnsApi.ArnsApiExtension.Companion.arnsApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.objectMapper
import java.time.LocalDateTime
import java.time.Year
import java.time.temporal.TemporalAdjusters

class ArnsApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    companion object {
        lateinit var arnsApi: ArnsApiMockServer
    }

    override fun beforeAll(context: ExtensionContext?) {
        arnsApi = ArnsApiMockServer()
    }

    override fun beforeEach(context: ExtensionContext?) {
        arnsApi.reset()
    }

    override fun afterAll(context: ExtensionContext?) {
        arnsApi.stop()
    }
}

class ArnsApiMockServer : ClientAndServer(MOCKSERVER_PORT) {

    companion object {
        private const val MOCKSERVER_PORT = 8094
    }

    fun getNeeds(crn: String, needs: List<Pair<Need, NeedSeverity>>) {
        val request = HttpRequest.request().withPath("/needs/crn/$crn")
        val response = AssessedNeeds(needs.map { it.assessedNeed() })
        arnsApi.`when`(request, Times.exactly(1)).respond(
            HttpResponse.response().withContentType(MediaType.APPLICATION_JSON)
                .withBody(objectMapper().writeValueAsString(response))
        )
    }

    fun getNoSeverityNeeds(crn: String) {
        val request = HttpRequest.request().withPath("/needs/crn/$crn")
        arnsApi.`when`(request, Times.exactly(1)).respond(
            HttpResponse.response().withContentType(MediaType.APPLICATION_JSON)
                .withBody(
                    objectMapper().writeValueAsString(AssessedNeeds(listOf((Need.ACCOMMODATION to NeedSeverity.NO_NEED).assessedNeed())))
                )
        )
    }

    fun getNotFoundNeeds(crn: String) {
        val request = HttpRequest.request().withPath("/needs/crn/$crn")
        arnsApi.`when`(request, Times.exactly(1)).respond(HttpResponse.notFoundResponse())
    }

    fun getHighSeverityNeeds(crn: String) {
        val request = HttpRequest.request().withPath("/needs/crn/$crn")
        arnsApi.`when`(request, Times.exactly(1)).respond(
            HttpResponse.response()
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(
                    objectMapper().writeValueAsString(
                        AssessedNeeds(Need.entries.map { (it to NeedSeverity.SEVERE).assessedNeed() })
                    )
                )
        )
    }

    fun getCurrentAssessment(crn: String, assessmentId: Long) {
        val request = HttpRequest.request().withPath("/assessments/timeline/crn/$crn")
        arnsApi.`when`(request, Times.exactly(1)).respond(
            HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(
                objectMapper().writeValueAsString(
                    Timeline(
                        listOf(
                            AssessmentSummary(assessmentId, getStartOfYear(Year.now().value), "LAYER3", "COMPLETE")
                        )
                    )
                )
            )
        )
    }

    fun getAssessment(crn: String, assessment: AssessmentSummary) {
        val request = HttpRequest.request().withPath("/assessments/timeline/crn/$crn")
        arnsApi.`when`(request, Times.exactly(1)).respond(
            HttpResponse.response().withContentType(MediaType.APPLICATION_JSON)
                .withBody(objectMapper().writeValueAsString(Timeline(listOf(assessment))))
        )
    }

    fun getOutdatedAssessment(crn: String, assessmentId: Long) {
        val request = HttpRequest.request().withPath("/assessments/timeline/crn/$crn")
        arnsApi.`when`(request, Times.exactly(1)).respond(
            HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(
                objectMapper().writeValueAsString(
                    Timeline(
                        listOf(
                            AssessmentSummary(
                                assessmentId,
                                getStartOfYear(2018),
                                "LAYER3",
                                "COMPLETE",
                            ),
                            AssessmentSummary(
                                1235,
                                getStartOfYear(2018),
                                "LAYER3",
                                "INCOMPLETE_LOCKED"
                            ),
                        )
                    )
                )
            )
        )
    }

    fun getNotFoundAssessment(crn: String) {
        val request = HttpRequest.request().withPath("/assessments/timeline/crn/$crn")
        arnsApi.`when`(request, Times.exactly(1))
            .respond(HttpResponse.notFoundResponse().withContentType(MediaType.APPLICATION_JSON))
    }

    private fun getStartOfYear(year: Int): LocalDateTime =
        LocalDateTime.now().withYear(year).with(TemporalAdjusters.firstDayOfYear())

    private fun Pair<Need, NeedSeverity>.assessedNeed() = AssessedNeed(
        first.name,
        first.name,
        false,
        true,
        true,
        true,
        second,
        true,
        2
    )
}
