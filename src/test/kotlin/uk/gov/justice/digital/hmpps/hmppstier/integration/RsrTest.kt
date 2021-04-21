package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.MockedEndpointsTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.emptyRegistrationsResponse
import java.math.BigDecimal

class RsrTest : MockedEndpointsTestBase() {

  @Test
  fun `Given an RSR score of 7 point 1 And no ROSH score When a tier is calculated Then 20 points are scored So Protect is B`() {
    val crn = "X333445"
    setupSCCustodialSentence(crn)
    setupRegistrations(emptyRegistrationsResponse(), crn)
    setupCommunityApiAssessment(crn, BigDecimal(7.1))
    setupMaleOffender(crn)
    calculateTierFor(crn)
    expectTierCalculation("B2")
  }
}
