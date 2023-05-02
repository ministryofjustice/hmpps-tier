package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockserver.integration.ClientAndServer

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
class AssessmentApiMockServer: ClientAndServer(MOCKSERVER_PORT) {

  companion object {
    private const val MOCKSERVER_PORT = 8092
  }
}