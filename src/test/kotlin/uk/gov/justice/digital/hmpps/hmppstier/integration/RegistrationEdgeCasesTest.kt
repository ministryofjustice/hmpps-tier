package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.MockedEndpointsTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.assessmentsApiNoSeverityNeedsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.emptyRegistrationsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithMappa
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithNoLevel

class RegistrationEdgeCasesTest : MockedEndpointsTestBase() {

  @Test
  fun `calculate change and protect when no registrations are found`() {
    val crn = "X373878"
    setupNCCustodialSentence(crn)
    setupRegistrations(emptyRegistrationsResponse(), crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn)
    calculateTierFor(crn)
    expectTierCalculation("B1")
  }

  @Test
  fun `calculate change and protect when registration level is missing`() {
    val crn = "X445599"
    setupNCCustodialSentence(crn)
    setupRegistrations(registrationsResponseWithNoLevel(), crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn)
    calculateTierFor(crn)
    expectTierCalculation("B1")
  }

  @Test
  fun `uses latest registration - two mappa registrations present`() {
    val crn = "X445599"
    setupNCCustodialSentence(crn)
    setupRegistrations(registrationsResponseWithMappa(), crn)
    setupMaleOffender(crn)
    setupNeeds(assessmentsApiNoSeverityNeedsResponse())
    setupNoDeliusAssessment(crn)
    calculateTierFor(crn)
    expectTierCalculation("A2")
  }
}
