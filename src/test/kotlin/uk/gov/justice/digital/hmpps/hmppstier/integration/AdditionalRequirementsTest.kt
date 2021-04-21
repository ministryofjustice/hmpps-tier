package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.MockedEndpointsTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponse

class AdditionalRequirementsTest : MockedEndpointsTestBase() {

  @Test
  fun `Additional requirements do not cause a processing error`() {
    val crn = "X833444"
    setupNonCustodialSentence(crn)
    setupRegistrations(registrationsResponse(), crn)
    setupAdditionalRequirements(crn)
    restOfSetupWithMaleOffenderAndSevereNeeds(crn)
    calculateTierFor(crn)
    expectTierCalculation("A0")
  }
}
