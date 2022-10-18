package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithMappa

class NoAssessmentFoundTest : IntegrationTestBase() {

  @Test
  fun `changeLevel should be 2 if assessment returns 404`() {
    val crn = "X273878"
    setupNCCustodialSentence(crn)
    setupAssessmentNotFound(crn)
    setupRegistrations(registrationsResponseWithMappa(), crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = "8234567890")
    calculateTierFor(crn)
    expectTierChangedById("A2")
  }
}
