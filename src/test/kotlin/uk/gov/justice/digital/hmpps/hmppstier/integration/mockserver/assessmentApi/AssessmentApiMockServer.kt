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
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.response.domain.Answer

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

    fun getAnswers(assessmentId: Long, answers: Collection<Answer>) {
        val request = HttpRequest.request().withPath("/assessments/oasysSetId/$assessmentId/answers")
        assessmentApi.`when`(request, Times.exactly(1)).respond(
            HttpResponse.response().withContentType(MediaType.APPLICATION_JSON)
                .withBody(answersResponse(assessmentId, *answers.toTypedArray())),
        )
    }
}
