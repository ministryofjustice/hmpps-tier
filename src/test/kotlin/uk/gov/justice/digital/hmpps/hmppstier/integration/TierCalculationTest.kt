package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType.APPLICATION_JSON
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppstier.controller.TierCalculationRequiredEventListener

@TestInstance(PER_CLASS)
class TierCalculationTest : MockedEndpointsTestBase() {

  @Autowired
  lateinit var listener: TierCalculationRequiredEventListener

  @Test
  fun `calculate change and protect for SC custodial sentence`() {
    val crn = "X373878"
    setupSCCustodialSentence(crn)
    setupRestWithRegistrations(crn)

    val expectedTierUpdate = setupUpdateTierSuccess(crn, "A1")

    listener.listen(calculationMessage(crn))

    mockCommunityApiServer.verify(expectedTierUpdate)
  }

  @Test
  fun `calculate change and protect for NC custodial sentence`() {
    val crn = "123"
    setupNCCustodialSentence(crn)
    setupRestWithRegistrations(crn)
    val expectedTierUpdate = setupUpdateTierSuccess(crn, "A1")

    listener.listen(calculationMessage(crn))

    mockCommunityApiServer.verify(expectedTierUpdate)
  }

  @Test
  fun `do not calculate change for terminated custodial sentence`() {
    val crn = "X373878"
    setupTerminatedCustodialSentence(crn)
    setupRestWithRegistrations(crn)

    val expectedTierUpdate = setupUpdateTierSuccess(crn, "A0")

    listener.listen(calculationMessage(crn))

    mockCommunityApiServer.verify(expectedTierUpdate)
  }

  @Test
  fun `calculate change for terminated non-custodial sentence with unpaid work and current non-custodial sentence`() {
    val crn = "X505050"
    setupCurrentNonCustodialSentenceAndTerminatedNonCustodialSentenceWithUnpaidWork(crn)
    setupNonRestrictiveRequirements(crn)
    setupRestWithRegistrations(crn)

    val expectedTierUpdate = setupUpdateTierSuccess(crn, "A1")

    listener.listen(calculationMessage(crn))

    mockCommunityApiServer.verify(expectedTierUpdate)
  }

  @Test
  fun `calculate change and protect for non-custodial sentence with no restrictive requirements or unpaid work`() {
    val crn = "X222222"

    setupNonCustodialSentenceWithNoUnpaidWork(crn)
    setupNonRestrictiveRequirements(crn)
    setupRestWithRegistrations(crn)

    val expectedTierUpdate = setupUpdateTierSuccess(crn, "A1")

    listener.listen(calculationMessage(crn))

    mockCommunityApiServer.verify(expectedTierUpdate)
  }

  @Test
  fun `do not calculate change for terminated non-custodial sentence with no restrictive requirements or unpaid work`() {
    val crn = "X888888"

    setupTerminatedNonCustodialSentenceWithNoUnpaidWork(crn)
    setupNonRestrictiveRequirements(crn)
    setupRestWithRegistrations(crn)

    val expectedTierUpdate = setupUpdateTierSuccess(crn, "A0")

    listener.listen(calculationMessage(crn))

    mockCommunityApiServer.verify(expectedTierUpdate)
  }

  @Test
  fun `do not calculate change for a non-custodial sentence with unpaid work`() {
    val crn = "X232323"

    setupNonCustodialSentenceWithUnpaidWork(crn)
    setupNonRestrictiveRequirements(crn)
    setupRestWithRegistrations(crn)

    val expectedTierUpdate = setupUpdateTierSuccess(crn, "A0")

    listener.listen(calculationMessage(crn))

    mockCommunityApiServer.verify(expectedTierUpdate)
  }

  @Test
  fun `do not calculate change for non-custodial sentence with restrictive requirements`() {
    val crn = "X989898"

    setupNonCustodialSentenceWithNoUnpaidWork(crn)
    setupRestrictiveRequirements(crn)
    setupRestWithRegistrations(crn)

    val expectedTierUpdate = setupUpdateTierSuccess(crn, "A0")

    listener.listen(calculationMessage(crn))

    mockCommunityApiServer.verify(expectedTierUpdate)
  }

  @Test
  fun `calculate change for concurrent custodial and non-custodial sentence with unpaid work`() {
    val crn = "X676767"

    setupConcurrentCustodialAndNonCustodialSentenceWithUnpaidWork(crn)
    setupRestrictiveRequirements(crn)
    setupRestWithRegistrations(crn)

    val expectedTierUpdate = setupUpdateTierSuccess(crn, "A1")

    listener.listen(calculationMessage(crn))

    mockCommunityApiServer.verify(expectedTierUpdate)
  }

  private fun setupRestWithRegistrations(crn: String) {
    setupRegistrations(ApiResponses.registrationsResponse(), crn)
    restOfSetup(crn)
  }

  private fun setupNonCustodialSentenceWithNoUnpaidWork(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/$crn/convictions")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.nonCustodialConvictionResponse())
    )
  }

  private fun setupCurrentNonCustodialSentenceAndTerminatedNonCustodialSentenceWithUnpaidWork(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/$crn/convictions")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.nonCustodialCurrentAndTerminatedConvictionWithUnpaidWorkResponse())
    )
  }

  private fun setupConcurrentCustodialAndNonCustodialSentenceWithUnpaidWork(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/$crn/convictions")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.custodialAndNonCustodialUnpaid())
    )
  }

  private fun setupNonCustodialSentenceWithUnpaidWork(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/$crn/convictions")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.nonCustodialUnpaidWorkConvictionResponse())
    )
  }

  private fun setupSCCustodialSentence(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/$crn/convictions")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.custodialSCConvictionResponse())
    )
  }

  private fun setupTerminatedCustodialSentence(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/$crn/convictions")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.custodialTerminatedConvictionResponse())
    )
  }

  private fun setupTerminatedNonCustodialSentenceWithNoUnpaidWork(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/$crn/convictions")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.nonCustodialTerminatedConvictionResponse())
    )
  }

  private fun setupRestrictiveRequirements(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/$crn/convictions/\\d+/requirements")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.restrictiveRequirementsResponse())
    )
  }

  private fun setupNonRestrictiveRequirements(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/offenders/crn/$crn/convictions/\\d+/requirements")).respond(
      response().withContentType(
        APPLICATION_JSON
      ).withBody(ApiResponses.nonRestrictiveRequirementsResponse())
    )
  }
}
