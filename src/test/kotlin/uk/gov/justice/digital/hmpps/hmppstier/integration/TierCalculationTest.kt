package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.mockserver.model.HttpRequest.request
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppstier.controller.TierCalculationRequiredEventListener
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.custodialSCConvictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.custodialTerminatedConvictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.emptyNsiResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.nonCustodialConvictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.nonCustodialCurrentAndTerminatedConvictionWithUnpaidWorkResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.nonCustodialTerminatedConvictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.nonCustodialUnpaidWorkConvictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.nonRestrictiveRequirementsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.registrationsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.restrictiveRequirementsResponse

@TestInstance(PER_CLASS)
class TierCalculationTest : MockedEndpointsTestBase() {

  @Autowired
  lateinit var listener: TierCalculationRequiredEventListener

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
      val expectedTierUpdate = setupUpdateTierSuccess(crn, "D2")

      listener.listen(calculationMessage(crn))

      mockCommunityApiServer.verify(expectedTierUpdate)
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
    fun `calculate change and protect for SC custodial sentence`() {
      val crn = "X373878"
      setupSCCustodialSentence(crn)
      setupMaleOffenderWithRegistrations(crn)

      val expectedTierUpdate = setupUpdateTierSuccess(crn, "A1")

      listener.listen(calculationMessage(crn))

      mockCommunityApiServer.verify(expectedTierUpdate)
    }

    @Test
    fun `calculate change and protect for NC custodial sentence`() {
      val crn = "123"
      setupNCCustodialSentence(crn)
      setupMaleOffenderWithRegistrations(crn)
      val expectedTierUpdate = setupUpdateTierSuccess(crn, "A1")

      listener.listen(calculationMessage(crn))

      mockCommunityApiServer.verify(expectedTierUpdate)
    }

    @Test
    fun `do not calculate change for terminated custodial sentence`() {
      val crn = "X373878"
      setupTerminatedCustodialSentence(crn)
      setupMaleOffenderWithRegistrations(crn)

      val expectedTierUpdate = setupUpdateTierSuccess(crn, "A0")

      listener.listen(calculationMessage(crn))

      mockCommunityApiServer.verify(expectedTierUpdate)
    }

    @Test
    fun `calculate change for terminated non-custodial sentence with unpaid work and current non-custodial sentence`() {
      val crn = "X505050"
      setupCurrentNonCustodialSentenceAndTerminatedNonCustodialSentenceWithUnpaidWork(crn)
      setupNonRestrictiveRequirements(crn)
      setupMaleOffenderWithRegistrations(crn)

      val expectedTierUpdate = setupUpdateTierSuccess(crn, "A1")

      listener.listen(calculationMessage(crn))

      mockCommunityApiServer.verify(expectedTierUpdate)
    }

    @Test
    fun `calculate change and protect for non-custodial sentence with no restrictive requirements or unpaid work`() {
      val crn = "X222222"

      setupNonCustodialSentenceWithNoUnpaidWork(crn)
      setupNonRestrictiveRequirements(crn)
      setupMaleOffenderWithRegistrations(crn)

      val expectedTierUpdate = setupUpdateTierSuccess(crn, "A1")

      listener.listen(calculationMessage(crn))

      mockCommunityApiServer.verify(expectedTierUpdate)
    }

    @Test
    fun `do not calculate change for terminated non-custodial sentence with no restrictive requirements or unpaid work`() {
      val crn = "X888888"

      setupTerminatedNonCustodialSentenceWithNoUnpaidWork(crn)
      setupNonRestrictiveRequirements(crn)
      setupMaleOffenderWithRegistrations(crn)

      val expectedTierUpdate = setupUpdateTierSuccess(crn, "A0")

      listener.listen(calculationMessage(crn))

      mockCommunityApiServer.verify(expectedTierUpdate)
    }

    @Test
    fun `do not calculate change for a non-custodial sentence with unpaid work`() {
      val crn = "X232323"

      setupNonCustodialSentenceWithUnpaidWork(crn)
      setupNonRestrictiveRequirements(crn)
      setupMaleOffenderWithRegistrations(crn)

      val expectedTierUpdate = setupUpdateTierSuccess(crn, "A0")

      listener.listen(calculationMessage(crn))

      mockCommunityApiServer.verify(expectedTierUpdate)
    }

    @Test
    fun `do not calculate change for non-custodial sentence with restrictive requirements`() {
      val crn = "X989898"

      setupNonCustodialSentenceWithNoUnpaidWork(crn)
      setupRestrictiveRequirements(crn)
      setupMaleOffenderWithRegistrations(crn)

      val expectedTierUpdate = setupUpdateTierSuccess(crn, "A0")

      listener.listen(calculationMessage(crn))

      mockCommunityApiServer.verify(expectedTierUpdate)
    }

    @Test
    fun `calculate change for concurrent custodial and non-custodial sentence with unpaid work`() {
      val crn = "X676767"

      setupConcurrentCustodialAndNonCustodialSentenceWithUnpaidWork(crn)
      setupRestrictiveRequirements(crn)
      setupMaleOffenderWithRegistrations(crn)

      val expectedTierUpdate = setupUpdateTierSuccess(crn, "A1")

      listener.listen(calculationMessage(crn))

      mockCommunityApiServer.verify(expectedTierUpdate)
    }

    @Test
    fun `default change to '2' for non recent assessment`() {
      val crn = "X432768"

      setupSCCustodialSentence(crn)
      setupMaleOffenderWithRegistrations(crn, includeAssessmentApi = false)
      setupLatestAssessment(crn, 2018)

      val expectedTierUpdate = setupUpdateTierSuccess(crn, "A2")

      listener.listen(calculationMessage(crn))

      mockCommunityApiServer.verify(expectedTierUpdate)
    }

    private fun setupMaleOffenderWithRegistrations(crn: String, includeAssessmentApi: Boolean = true) {
      setupRegistrations(registrationsResponse(), crn)
      restOfSetupWithMaleOffender(crn, includeAssessmentApi)
    }

    private fun setupSCCustodialSentence(crn: String) {
      mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions")).respond(
        jsonResponseOf(custodialSCConvictionResponse())
      )
    }

    private fun setupNonCustodialSentenceWithNoUnpaidWork(crn: String) {
      mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions")).respond(
        jsonResponseOf(nonCustodialConvictionResponse())
      )
    }

    private fun setupCurrentNonCustodialSentenceAndTerminatedNonCustodialSentenceWithUnpaidWork(crn: String) {
      mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions")).respond(
        jsonResponseOf(nonCustodialCurrentAndTerminatedConvictionWithUnpaidWorkResponse())
      )
    }

    private fun setupConcurrentCustodialAndNonCustodialSentenceWithUnpaidWork(crn: String) {
      mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions")).respond(
        jsonResponseOf(ApiResponses.custodialAndNonCustodialUnpaid())
      )
    }

    private fun setupNonCustodialSentenceWithUnpaidWork(crn: String) {
      mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions")).respond(
        jsonResponseOf(nonCustodialUnpaidWorkConvictionResponse())
      )
    }

    private fun setupTerminatedCustodialSentence(crn: String) {
      mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions")).respond(
        jsonResponseOf(custodialTerminatedConvictionResponse())
      )
    }

    private fun setupTerminatedNonCustodialSentenceWithNoUnpaidWork(crn: String) {
      mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions")).respond(
        jsonResponseOf(nonCustodialTerminatedConvictionResponse())
      )
    }

    private fun setupRestrictiveRequirements(crn: String) {
      mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions/\\d+/requirements"))
        .respond(
          jsonResponseOf(restrictiveRequirementsResponse())
        )
    }

    private fun setupNonRestrictiveRequirements(crn: String) {
      mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions/\\d+/requirements"))
        .respond(
          jsonResponseOf(nonRestrictiveRequirementsResponse())
        )
    }
  }
}
