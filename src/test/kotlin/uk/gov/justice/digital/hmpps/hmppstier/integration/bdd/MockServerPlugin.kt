package uk.gov.justice.digital.hmpps.hmppstier.integration.bdd

import io.cucumber.plugin.EventListener
import io.cucumber.plugin.event.EventPublisher
import io.cucumber.plugin.event.TestCaseStarted
import io.cucumber.plugin.event.TestRunFinished
import io.cucumber.plugin.event.TestRunStarted
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.arnsApi.ArnsApiExtension
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.hmppsAuth.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension

class MockServerPlugin : EventListener {

    private var tierToDeliusApiExtension: TierToDeliusApiExtension = TierToDeliusApiExtension()
    private var hmppsAuthApiExtension: HmppsAuthApiExtension = HmppsAuthApiExtension()
    private var arnsApiExtension: ArnsApiExtension = ArnsApiExtension()

    override fun setEventPublisher(publisher: EventPublisher?) {
        publisher!!.registerHandlerFor(TestRunStarted::class.java) {
            testRunStarted()
        }
        publisher.registerHandlerFor(TestRunFinished::class.java) {
            testRunFinished()
        }
        publisher.registerHandlerFor(TestCaseStarted::class.java) {
            testCaseStarted()
        }
    }

    fun testRunStarted() {
        tierToDeliusApiExtension.beforeAll(null)
        hmppsAuthApiExtension.beforeAll(null)
        arnsApiExtension.beforeAll(null)
    }

    fun testCaseStarted() {
        tierToDeliusApiExtension.beforeEach(null)
        hmppsAuthApiExtension.beforeEach(null)
        arnsApiExtension.beforeEach(null)
    }

    fun testRunFinished() {
        tierToDeliusApiExtension.afterAll(null)
        hmppsAuthApiExtension.afterAll(null)
        arnsApiExtension.afterAll(null)
    }
}
