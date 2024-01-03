package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.AssessmentApiExtension.Companion.assessmentApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.response.answersResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.response.assessmentsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.response.domain.Answer
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.response.domain.Assessment
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.response.domain.Need
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.response.needsResponse
import java.time.LocalDateTime
import java.time.Year
import java.time.temporal.TemporalAdjusters.firstDayOfYear

class AssessmentApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    companion object {
        lateinit var assessmentApi: AssessmentApiMockServer
    }

    override fun beforeAll(context: ExtensionContext?) {
        assessmentApi = AssessmentApiMockServer()
    }

    override fun beforeEach(context: ExtensionContext?) {
        assessmentApi.reset()
    }

    override fun afterAll(context: ExtensionContext?) {
        assessmentApi.stop()
    }
}

class AssessmentApiMockServer : ClientAndServer(MOCKSERVER_PORT) {

    companion object {
        private const val MOCKSERVER_PORT = 8092
    }

    fun getNeeds(assessmentId: Long, needs: List<Need>) {
        val request = HttpRequest.request().withPath("/assessments/oasysSetId/$assessmentId/needs")
        assessmentApi.`when`(request, Times.exactly(1)).respond(
            HttpResponse.response().withContentType(MediaType.APPLICATION_JSON)
                .withBody(needsResponse(*needs.toTypedArray())),
        )
    }

    fun getNoSeverityNeeds(assessmentId: Long) {
        val request = HttpRequest.request().withPath("/assessments/oasysSetId/$assessmentId/needs")
        assessmentApi.`when`(request, Times.exactly(1)).respond(
            HttpResponse.response().withContentType(MediaType.APPLICATION_JSON)
                .withBody(needsResponse(Need("Accommodation", "ACCOMMODATION", "NO_NEED"))),
        )
    }

    fun getNotFoundNeeds(assessmentId: Long) {
        val request = HttpRequest.request().withPath("/assessments/oasysSetId/$assessmentId/needs")
        assessmentApi.`when`(request, Times.exactly(1)).respond(HttpResponse.notFoundResponse())
    }

    fun getHighSeverityNeeds(assessmentId: Long) {
        val request = HttpRequest.request().withPath("/assessments/oasysSetId/$assessmentId/needs")
        assessmentApi.`when`(request, Times.exactly(1)).respond(
            HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(
                needsResponse(
                    Need("Accommodation", "ACCOMMODATION", "SEVERE"),
                    Need("Education, Training and Employability", "EDUCATION_TRAINING_AND_EMPLOYABILITY", "SEVERE"),
                    Need("Relationships", "RELATIONSHIPS", "SEVERE"),
                    Need("Lifestyle and Associates", "LIFESTYLE_AND_ASSOCIATES", "SEVERE"),
                    Need("Drug Misuse", "DRUG_MISUSE", "SEVERE"),
                    Need("Thinking and Behaviour", "THINKING_AND_BEHAVIOUR", "SEVERE"),
                    Need("Attitudes", "ATTITUDES", "SEVERE"),
                    Need("Financial Management and Income", "FINANCIAL_MANAGEMENT_AND_INCOME", "SEVERE"),
                    Need("Emotional Well-Being", "EMOTIONAL_WELL_BEING", "SEVERE"),
                ),
            ),
        )
    }

    fun getAnswers(assessmentId: Long, answers: Collection<Answer>) {
        val request = HttpRequest.request().withPath("/assessments/oasysSetId/$assessmentId/answers")
        assessmentApi.`when`(request, Times.exactly(1)).respond(
            HttpResponse.response().withContentType(MediaType.APPLICATION_JSON)
                .withBody(answersResponse(assessmentId, *answers.toTypedArray())),
        )
    }

    fun getCurrentAssessment(crn: String, assessmentId: Long) {
        val request = HttpRequest.request().withPath("/offenders/crn/$crn/assessments/summary")
        assessmentApi.`when`(request, Times.exactly(1)).respond(
            HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(
                assessmentsResponse(
                    Assessment(
                        getStartOfYear(Year.now().value),
                        assessmentId,
                        "COMPLETE",
                    ),
                    Assessment(getStartOfYear(Year.now().value), 1235, "INCOMPLETE_LOCKED"),
                ),
            ),
        )
    }

    fun getAssessment(crn: String, assessment: Assessment) {
        val request = HttpRequest.request().withPath("/offenders/crn/$crn/assessments/summary")
        assessmentApi.`when`(request, Times.exactly(1)).respond(
            HttpResponse.response().withContentType(MediaType.APPLICATION_JSON)
                .withBody(assessmentsResponse(assessment)),
        )
    }

    fun getOutdatedAssessment(crn: String, assessmentId: Long) {
        val request = HttpRequest.request().withPath("/offenders/crn/$crn/assessments/summary")
        assessmentApi.`when`(request, Times.exactly(1)).respond(
            HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(
                assessmentsResponse(
                    Assessment(
                        getStartOfYear(2018),
                        assessmentId,
                        "COMPLETE",
                    ),
                    Assessment(getStartOfYear(2018), 1235, "INCOMPLETE_LOCKED"),
                ),
            ),
        )
    }

    fun getNotFoundAssessment(crn: String) {
        val request = HttpRequest.request().withPath("/offenders/crn/$crn/assessments/summary")
        assessmentApi.`when`(request, Times.exactly(1))
            .respond(HttpResponse.notFoundResponse().withContentType(MediaType.APPLICATION_JSON))
    }

    private fun getStartOfYear(year: Int): LocalDateTime = LocalDateTime.now().withYear(year).with(firstDayOfYear())
}
