package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithMappa

class AdditionalRequirementsTest : IntegrationTestBase() {

  @Test
  fun `Additional requirements do not cause a processing error`() {
    val crn = "X833444"
    setupNonCustodialSentence(crn)
    setupRegistrations(registrationsResponseWithMappa(), crn)
    setupAdditionalRequirements(crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = "4234567890")
    calculateTierFor(crn)
    expectTierChangedById("A0")
  }
}
