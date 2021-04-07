package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.MockedEndpointsTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponse

@TestInstance(PER_CLASS)
class CannotCalculateTierTest : MockedEndpointsTestBase() {

  @Test
  fun `Offender does not exist`() {
    val crn = "X123456"
    setupSCCustodialSentence(crn)
    setupRegistrations(registrationsResponse(), crn)
    restOfSetupWithMaleOffenderAndSevereNeeds("NOTFOUND")
    calculateTierFor(crn)
    expectNoTierCalculation()
  }
}
