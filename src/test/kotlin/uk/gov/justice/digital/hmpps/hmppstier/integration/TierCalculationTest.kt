package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.MockedEndpointsTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.emptyRegistrationsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseMappaThirty

class TierCalculationTest : MockedEndpointsTestBase() {

  @Nested
  inner class FemaleOffender {
    @Test
    fun `no NSis returned`() {
      val crn = "X386786"
      setupAssessmentNotFound(crn)

      setupNCCustodialSentence(crn)
      setupRegistrations(emptyRegistrationsResponse(), crn)

      restOfSetupWithFemaleOffender(crn)
      setupEmptyNsisResponse(crn)

      calculateTierFor(crn)
      expectTierCalculation("D2")
    }
  }

  @Nested
  inner class MaleOffender {

    @Test
    fun `default change to '2' for non recent assessment`() {
      val crn = "X432768"

      setupSCCustodialSentence(crn)
      setupMaleOffenderWithRegistrations(crn, includeAssessmentApi = false)
      setupLatestAssessment(crn, 2018)

      calculateTierFor(crn)
      expectTierCalculation("A2")
    }

    @Test
    fun `change score 2 for 10 points`() {
      val crn = "X432768"

      setupSCCustodialSentence(crn)
      setupRegistrations(registrationsResponseMappaThirty(), crn)
      restOfSetupWithMaleOffenderAnd8PointNeeds(crn, true)

      calculateTierFor(crn)
      expectTierCalculation("A2")
    }
  }
}
