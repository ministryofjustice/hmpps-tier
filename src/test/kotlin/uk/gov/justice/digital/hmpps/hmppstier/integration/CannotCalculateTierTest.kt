package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase

class CannotCalculateTierTest : IntegrationTestBase() {

  @Test
  fun `Offender does not exist`() {
    val crn = "X123456"
    setupTierToDeliusNotFound(crn)
    calculateTierFor(crn)
    expectTierCalculationToHaveFailed()
  }
}
