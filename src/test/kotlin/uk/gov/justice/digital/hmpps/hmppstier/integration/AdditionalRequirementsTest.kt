package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.MockedEndpointsTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithMappa

class AdditionalRequirementsTest : MockedEndpointsTestBase() {

  @Test
  fun `Additional requirements do not cause a processing error`() {
    val crn = "X833444"
    setupNonCustodialSentence(crn)
    setupRegistrations(registrationsResponseWithMappa(), crn)
    setupAdditionalRequirements(crn)
    restOfSetupWithMaleOffenderAndSevereNeeds(crn, assessmentId = "4234567890")
    calculateTierFor(crn)
    expectTierCalculation("A0")
  }
}
