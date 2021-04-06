package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.MockedEndpointsTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponse

@TestInstance(PER_CLASS)
class AdditionalRequirementsTest : MockedEndpointsTestBase() {

  @Test
  fun `Additional requirements do not cause a processing error`() {
    val crn = "X333444"
    setupNonCustodialSentence(crn)
    setupRegistrations(registrationsResponse(), crn)
    setupAdditionalRequirements(crn)
    restOfSetupWithMaleOffenderAndSevereNeeds(crn)
    calculateTierFor(crn)
    expectTierCalculation("A3")
  }
}
