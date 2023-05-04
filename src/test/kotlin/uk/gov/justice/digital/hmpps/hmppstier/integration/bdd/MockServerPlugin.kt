package uk.gov.justice.digital.hmpps.hmppstier.integration.bdd

import io.cucumber.plugin.EventListener
import io.cucumber.plugin.event.EventPublisher
import io.cucumber.plugin.event.TestCaseStarted
import io.cucumber.plugin.event.TestRunFinished
import io.cucumber.plugin.event.TestRunStarted
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.AssessmentApiExtension
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.CommunityApiExtension
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.hmppsAuth.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension

class MockServerPlugin : EventListener {

  private var tierToDeliusApiExtension: TierToDeliusApiExtension = TierToDeliusApiExtension()
  private var hmppsAuthApiExtension: HmppsAuthApiExtension = HmppsAuthApiExtension()
  private var communityApiExtension: CommunityApiExtension = CommunityApiExtension()
  private var assessmentApiExtension: AssessmentApiExtension = AssessmentApiExtension()

  override fun setEventPublisher(publisher: EventPublisher?) {
    publisher!!.registerHandlerFor(TestRunStarted::class.java, this::testRunStarted)
    publisher.registerHandlerFor(TestRunFinished::class.java, this::testRunFinished)
    publisher.registerHandlerFor(TestCaseStarted::class.java, this::testCaseStarted)
  }

  fun testRunStarted(event: TestRunStarted) {
    tierToDeliusApiExtension.beforeAll(null)
    hmppsAuthApiExtension.beforeAll(null)
    communityApiExtension.beforeAll(null)
    assessmentApiExtension.beforeAll(null)
  }

  fun testCaseStarted(event: TestCaseStarted) {
    tierToDeliusApiExtension.beforeEach(null)
    hmppsAuthApiExtension.beforeEach(null)
    communityApiExtension.beforeEach(null)
    assessmentApiExtension.beforeEach(null)
  }

  fun testRunFinished(event: TestRunFinished) {
    tierToDeliusApiExtension.afterAll(null)
    hmppsAuthApiExtension.afterAll(null)
    communityApiExtension.afterAll(null)
    assessmentApiExtension.afterAll(null)
  }
}
