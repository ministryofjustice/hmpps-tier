package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockserver.integration.ClientAndServer

class TierToDeliusApiExtension: BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

  companion object {
    lateinit var tierToDeliusApi: TierToDeliusApiMockServer
  }

  override fun beforeAll(context: ExtensionContext?) {
    tierToDeliusApi = TierToDeliusApiMockServer()
  }

  override fun beforeEach(context: ExtensionContext?) {
    tierToDeliusApi.reset()
  }

  override fun afterAll(context: ExtensionContext?) {
    tierToDeliusApi.stop()
  }
}
class TierToDeliusApiMockServer: ClientAndServer(MOCKSERVER_PORT) {

  companion object {
    private const val MOCKSERVER_PORT = 8092
  }
}