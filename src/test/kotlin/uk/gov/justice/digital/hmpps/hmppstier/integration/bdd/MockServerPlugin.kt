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

    private val tierToDeliusApiExtension = TierToDeliusApiExtension()
    private val hmppsAuthApiExtension = HmppsAuthApiExtension()
    private val arnsApiExtension = ArnsApiExtension()

    override fun setEventPublisher(publisher: EventPublisher) {
        publisher.registerHandlerFor(TestRunStarted::class.java) {
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
        tierToDeliusApiExtension.start()
        hmppsAuthApiExtension.start()
        arnsApiExtension.start()
    }

    fun testCaseStarted() {
        tierToDeliusApiExtension.reset()
        hmppsAuthApiExtension.reset()
        arnsApiExtension.reset()
    }

    fun testRunFinished() {
        tierToDeliusApiExtension.stop()
        hmppsAuthApiExtension.stop()
        arnsApiExtension.stop()
    }
}
