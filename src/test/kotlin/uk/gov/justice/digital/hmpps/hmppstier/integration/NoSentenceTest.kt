package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.mockserver.model.HttpRequest.request
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppstier.controller.TierCalculationRequiredEventListener
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.noSentenceConvictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.registrationsResponse

@TestInstance(PER_CLASS)
class NoSentenceTest : MockedEndpointsTestBase() {

  @Autowired
  lateinit var listener: TierCalculationRequiredEventListener

  @Test
  fun `Tier is calculated with change level zero when no sentence is found`() {
    val crn = "X333444"
    setUpNoSentence(crn)
    setupRegistrations(registrationsResponse(), crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn)
    listener.listen(calculationMessage(crn))
  }

  private fun setUpNoSentence(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions")).respond(
      jsonResponseOf(noSentenceConvictionResponse())
    )
  }
}
