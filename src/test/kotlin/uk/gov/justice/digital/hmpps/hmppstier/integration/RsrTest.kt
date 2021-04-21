package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.MockedEndpointsTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponse

class RsrTest : MockedEndpointsTestBase() {

  @Test
  @Disabled
  fun `Given an RSR score of 7 point 1 And no ROSH score When a tier is calculated Then 20 points are scored`() {
    val crn = "X333445"
    setupSCCustodialSentence(crn)
    setupRegistrations(registrationsResponse(), crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn)
    calculateTierFor(crn)
    expectTierCalculation("B3")
  }
}
