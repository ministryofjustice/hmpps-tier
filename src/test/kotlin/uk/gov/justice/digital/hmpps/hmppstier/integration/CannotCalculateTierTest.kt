package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.MockedEndpointsTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.emptyRegistrationsResponse

class CannotCalculateTierTest : MockedEndpointsTestBase() {

  @Test
  fun `Offender does not exist`() {
    val crn = "X123456"
    setupCurrentAssessment(crn, "1234567890")
    setupSCCustodialSentence(crn)
    setupRegistrations(emptyRegistrationsResponse(), crn)
    setupCommunityApiAssessment(crn)
    setupMaleOffenderNotFound(crn)
    calculateTierFor(crn)
    expectTierCalculationToHaveFailed()
  }
}
