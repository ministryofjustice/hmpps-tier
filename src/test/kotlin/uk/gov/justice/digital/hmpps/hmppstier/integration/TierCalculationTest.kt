package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.MockedEndpointsTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.emptyRegistrationsResponse

class TierCalculationTest : MockedEndpointsTestBase() {

  @Nested
  inner class FemaleOffender {
    @Test
    fun `no NSis returned`() {
      val crn = "X386786"
      setupAssessmentNotFound(crn)

      setupNCCustodialSentence(crn)
      setupRegistrations(emptyRegistrationsResponse(), crn)

      restOfSetupWithFemaleOffender(crn, "2234567890")
      setupEmptyNsisResponse(crn)

      calculateTierFor(crn)
      expectTierCalculation("D2")
    }
  }

  @Nested
  inner class MaleOffender {

    @Test
    fun `default change to '2' for non recent assessment`() {
      val crn = "X432767"

      setupSCCustodialSentence(crn)
      setupMaleOffenderWithRegistrations(crn, false, "4234568890")
      setupLatestAssessment(crn, 2018, "1234567890")

      calculateTierFor(crn)
      expectTierCalculation("A2")
    }
  }

  @Nested
  inner class TierChangeWriteback {
    @Test
    fun `Does not write back when tier is unchanged`() {
      val crn = "X432769"

      setupSCCustodialSentence(crn)
      setupMaleOffenderWithRegistrations(crn, false, "4234568890")
      setupLatestAssessment(crn, 2018, "1234567890")

      calculateTierFor(crn)
      expectTierCalculation("A2")

      setupSCCustodialSentence(crn)
      setupMaleOffenderWithRegistrations(crn, false, "4234568890")
      setupLatestAssessment(crn, 2018, "1234567890")

      calculateTierFor(crn)
      expectNoUpdatedTierCalculation()
    }
    @Test
    fun `writes back when tier is changed`() {
      val crn = "X432770"

      setupSCCustodialSentence(crn)
      setupMaleOffenderWithRegistrations(crn, false, "4234568890")
      setupLatestAssessment(crn, 2018, "4234568890")

      calculateTierFor(crn)
      expectTierCalculation("A2")

      setupSCCustodialSentence(crn)
      setupMaleOffenderWithRegistrations(crn, false, "4234568891")

      setupCurrentAssessment(crn, "4234568891") // assessment not out of date
      calculateTierFor(crn)
      expectTierCalculation("A1")
    }
  }
}
