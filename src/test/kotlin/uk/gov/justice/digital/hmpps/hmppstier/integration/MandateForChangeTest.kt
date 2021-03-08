package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppstier.controller.TierCalculationRequiredEventListener

@TestInstance(PER_CLASS)
class MandateForChangeTest : MockedEndpointsTestBase() {

  @Autowired
  lateinit var listener: TierCalculationRequiredEventListener

  @Test
  fun `do not calculate change for a non-custodial sentence with unpaid work and only restrictive requirements`() {
    val crn = "X232323"

    setupNonCustodialSentenceWithUnpaidWork(crn)
    setupRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)

    val expectedTierUpdate = tierUpdateWillSucceed(crn, "A0")

    listener.listen(calculationMessage(crn))

    mockCommunityApiServer.verify(expectedTierUpdate)
  }

  @Test
  fun `do not calculate change for non-custodial sentence with restrictive requirements`() {
    val crn = "X989898"

    setupNonCustodialSentenceWithNoUnpaidWork(crn)
    setupRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)

    val expectedTierUpdate = tierUpdateWillSucceed(crn, "A0")

    listener.listen(calculationMessage(crn))

    mockCommunityApiServer.verify(expectedTierUpdate)
  }

  @Test
  fun `calculate change for concurrent custodial and non-custodial sentence with unpaid work`() {
    val crn = "X676767"

    setupConcurrentCustodialAndNonCustodialSentenceWithUnpaidWork(crn)
    setupRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)

    val expectedTierUpdate = tierUpdateWillSucceed(crn, "A1")

    listener.listen(calculationMessage(crn))

    mockCommunityApiServer.verify(expectedTierUpdate)
  }

  @Test
  fun `calculate change and protect for SC custodial sentence`() {
    val crn = "X373878"
    setupSCCustodialSentence(crn)
    setupMaleOffenderWithRegistrations(crn)

    val expectedTierUpdate = tierUpdateWillSucceed(crn, "A1")

    listener.listen(calculationMessage(crn))

    mockCommunityApiServer.verify(expectedTierUpdate)
  }

  @Test
  fun `calculate change and protect for NC custodial sentence`() {
    val crn = "123"
    setupNCCustodialSentence(crn)
    setupMaleOffenderWithRegistrations(crn)
    val expectedTierUpdate = tierUpdateWillSucceed(crn, "A1")

    listener.listen(calculationMessage(crn))

    mockCommunityApiServer.verify(expectedTierUpdate)
  }

  @Test
  fun `do not calculate change for terminated custodial sentence`() {
    val crn = "X373878"
    setupTerminatedCustodialSentence(crn)
    setupMaleOffenderWithRegistrations(crn)

    val expectedTierUpdate = tierUpdateWillSucceed(crn, "A0")

    listener.listen(calculationMessage(crn))

    mockCommunityApiServer.verify(expectedTierUpdate)
  }

  @Test
  fun `calculate change for terminated non-custodial sentence with unpaid work and current non-custodial sentence`() {
    val crn = "X505050"
    setupCurrentNonCustodialSentenceAndTerminatedNonCustodialSentenceWithUnpaidWork(crn)
    setupNonRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)

    val expectedTierUpdate = tierUpdateWillSucceed(crn, "A1")

    listener.listen(calculationMessage(crn))

    mockCommunityApiServer.verify(expectedTierUpdate)
  }

  @Test
  fun `calculate change and protect for non-custodial sentence with no restrictive requirements or unpaid work`() {
    val crn = "X222222"

    setupNonCustodialSentenceWithNoUnpaidWork(crn)
    setupNonRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)

    val expectedTierUpdate = tierUpdateWillSucceed(crn, "A1")

    listener.listen(calculationMessage(crn))

    mockCommunityApiServer.verify(expectedTierUpdate)
  }

  @Test
  fun `do not calculate change for terminated non-custodial sentence with no restrictive requirements or unpaid work`() {
    val crn = "X888888"

    setupTerminatedNonCustodialSentenceWithNoUnpaidWork(crn)
    setupNonRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)

    val expectedTierUpdate = tierUpdateWillSucceed(crn, "A0")

    listener.listen(calculationMessage(crn))

    mockCommunityApiServer.verify(expectedTierUpdate)
  }

  @Test
  fun `do not calculate change when only restrictive requirements are present on a non-custodial sentence with no unpaid work`() {
    val crn = "X888866"

    setupNonCustodialSentenceWithNoUnpaidWork(crn)
    setupRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)

    val expectedTierUpdate = tierUpdateWillSucceed(crn, "A0")

    listener.listen(calculationMessage(crn))

    mockCommunityApiServer.verify(expectedTierUpdate)
  }
}
