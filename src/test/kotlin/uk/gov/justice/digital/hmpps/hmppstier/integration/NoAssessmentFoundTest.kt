package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.registrationsResponse

@TestInstance(PER_CLASS)
class NoAssessmentFoundTest : MockedEndpointsTestBase() {

  @Test
  fun `changeLevel should be 2 if assessment returns 404`() {
    val crn = "X373878"
    setupNCCustodialSentence(crn)
    setupAssessmentNotFound(crn)
    setupRegistrations(registrationsResponse(), crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn)
    putMessageOnQueue(offenderEventsAmazonSQSAsync, eventQueueUrl, crn)
    expectTierCalculation("A2")
  }
}
