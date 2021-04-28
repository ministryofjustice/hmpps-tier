package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.MockedEndpointsTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithMappa

class NoAssessmentFoundTest : MockedEndpointsTestBase() {

  @Test
  fun `changeLevel should be 2 if assessment returns 404`() {
    val crn = "X373878"
    setupNCCustodialSentence(crn)
    setupAssessmentNotFound(crn)
    setupRegistrations(registrationsResponseWithMappa(), crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn)
    calculateTierFor(crn)
    expectTierCalculation("A2")
  }
}
