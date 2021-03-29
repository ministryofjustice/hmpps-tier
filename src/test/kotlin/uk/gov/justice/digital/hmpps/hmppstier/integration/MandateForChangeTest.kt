package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS

@TestInstance(PER_CLASS)
class MandateForChangeTest : MockedEndpointsTestBase() {

  @Test
  fun `do not calculate change for a non-custodial sentence with only restrictive requirements`() {
    val crn = "X232323"

    setupNonCustodialSentenceWithNoUnpaidWork(crn)
    setupRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)

    calculateTierFor(crn)
    expectTierCalculation("A0")
  }

  @Test
  fun `calculate change for concurrent custodial and non-custodial sentence`() {
    val crn = "X676767"

    setupConcurrentCustodialAndNonCustodialSentenceWithUnpaidWork(crn)
    setupRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)

    calculateTierFor(crn)
    expectTierCalculation("A1")
  }

  @Test
  fun `calculate change and protect for SC custodial sentence`() {
    val crn = "X373878"
    setupSCCustodialSentence(crn)
    setupMaleOffenderWithRegistrations(crn)

    calculateTierFor(crn)
    expectTierCalculation("A1")
  }

  @Test
  fun `calculate change and protect for NC custodial sentence`() {
    val crn = "123"
    setupNCCustodialSentence(crn)
    setupMaleOffenderWithRegistrations(crn)

    calculateTierFor(crn)
    expectTierCalculation("A1")
  }

  @Test
  fun `do not calculate change for terminated custodial sentence`() {
    val crn = "X373878"
    setupTerminatedCustodialSentence(crn)
    setupMaleOffenderWithRegistrations(crn)

    calculateTierFor(crn)
    expectTierCalculation("A0")
  }

  @Test
  fun `calculate change for terminated non-custodial sentence and current non-custodial sentence with non-restrictive requirements`() {
    val crn = "X505050"
    setupCurrentNonCustodialSentenceAndTerminatedNonCustodialSentenceWithUnpaidWork(crn)
    setupNonRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)

    calculateTierFor(crn)
    expectTierCalculation("A1")
  }

  @Test
  fun `calculate change and protect for non-custodial sentence with non-restrictive requirements`() {
    val crn = "X222222"

    setupNonCustodialSentenceWithNoUnpaidWork(crn)
    setupNonRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)

    calculateTierFor(crn)
    expectTierCalculation("A1")
  }

  @Test
  fun `do not calculate change for terminated non-custodial sentence with non-restrictive requirements`() {
    val crn = "X888888"

    setupTerminatedNonCustodialSentenceWithNoUnpaidWork(crn)
    setupNonRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)

    calculateTierFor(crn)
    expectTierCalculation("A0")
  }

  @Test
  fun `do not calculate change when only restrictive requirements are present on a non-custodial sentence`() {
    val crn = "X888866"

    setupNonCustodialSentenceWithNoUnpaidWork(crn)
    setupRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)

    calculateTierFor(crn)
    expectTierCalculation("A0")
  }

  @Test
  fun `calculate change with restrictive and non-restrictive requirements on a non-custodial sentence`() {
    val crn = "X888855"

    setupNonCustodialSentenceWithNoUnpaidWork(crn)
    setupRestrictiveAndNonRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)

    calculateTierFor(crn)
    expectTierCalculation("A1")
  }

  @Test
  fun `do not calculate change when no requirements are present on a non-custodial sentence`() {
    val crn = "X888844"

    setupNonCustodialSentenceWithUnpaidWork(crn)
    setupNoRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)

    calculateTierFor(crn)
    expectTierCalculation("A0")
  }
}
