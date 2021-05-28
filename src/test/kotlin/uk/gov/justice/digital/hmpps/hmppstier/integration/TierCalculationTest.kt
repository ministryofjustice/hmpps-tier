package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.MockedEndpointsTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.assessmentsApiHighSeverityNeedsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.emptyRegistrationsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithMappa

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
  inner class TierChangeWriteback {
    @Test
    fun `Does not write back when tier is unchanged`() {
      val crn = "X432769"

      setupSCCustodialSentence(crn)
      setupRegistrations(registrationsResponseWithMappa(), crn)
      restOfSetupWithMaleOffenderNoSevereNeeds(crn, false, "4234568890")
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
    fun `Does not write back when calculation result differs but tier is unchanged`() {
      val crn = "X432779"

      setupSCCustodialSentence(crn)
      setupMaleOffenderWithRegistrations(crn, false, "4234568890")
      setupLatestAssessment(crn, 2018, "1234567890")

      calculateTierFor(crn)
      expectTierCalculation("A2")

      setupSCCustodialSentence(crn)
      setupRegistrations(registrationsResponseWithMappa(), crn)
      setupCommunityApiAssessment(crn, ogrs = "0")
      setupMaleOffender(crn)
      setupNeeds(assessmentsApiHighSeverityNeedsResponse(), "4234568899")
      setupCurrentAssessment(crn, "4234568899") // assessment not out of date

      calculateTierFor(crn)
      expectNoUpdatedTierCalculation()
    }

    @Test
    fun `writes back when change level is changed`() {
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

    @Test
    fun `writes back when protect level is changed`() {
      val crn = "X432771"

      setupSCCustodialSentence(crn)
      setupMaleOffenderWithRegistrations(crn, assessmentId = "4234568890")

      calculateTierFor(crn)
      expectTierCalculation("A1")

      setupSCCustodialSentence(crn)
      setupRegistrations(registrationsResponseWithMappa("M1"), crn)
      restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = "4234568890")

      calculateTierFor(crn)
      expectTierCalculation("B1")
    }
  }
}
