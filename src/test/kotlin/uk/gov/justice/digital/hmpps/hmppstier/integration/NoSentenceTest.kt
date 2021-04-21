package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.MockedEndpointsTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponse

class NoSentenceTest : MockedEndpointsTestBase() {

  @Test
  fun `Tier is calculated with change level zero when no sentence is found`() {
    val crn = "X333444"
    setUpNoSentence(crn)
    setupRegistrations(registrationsResponse(), crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn)
    calculateTierFor(crn)
    expectTierCalculation("A0")
  }
}
