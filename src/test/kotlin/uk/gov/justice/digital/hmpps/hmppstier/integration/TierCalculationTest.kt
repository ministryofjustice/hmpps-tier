package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.mockserver.model.HttpRequest.request
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.emptyNsiResponse

@TestInstance(PER_CLASS)
class TierCalculationTest : MockedEndpointsTestBase() {

  @Nested
  inner class FemaleOffender {
    @Test
    fun `no NSis returned`() {
      val crn = "X386786"
      setupAssessmentNotFound(crn)

      setupNCCustodialSentence(crn)
      setupRegistrations(ApiResponses.emptyRegistrationsResponse(), crn)

      restOfSetupWithFemaleOffender(crn)
      emptyNsisResponse(crn)

      calculateTierFor(crn)
      expectTierCalculation("D2")
    }
  }

  private fun emptyNsisResponse(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions/2500222290/nsis").withQueryStringParameter("nsiCodes", "BRE,BRES,REC,RECS")).respond(
      jsonResponseOf(emptyNsiResponse())
    )
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
      setupRegistrations(ApiResponses.registrationsResponse(), crn)
      restOfSetupWithMaleOffenderAnd8PointNeeds(crn, true)

      calculateTierFor(crn)
      expectTierCalculation("A2")
    }
  }
}
