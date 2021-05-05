package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.MockedEndpointsTestBase

class MandateForChangeTest : MockedEndpointsTestBase() {

  @Test
  fun `do not calculate change for a non-custodial sentence with only restrictive requirements`() {
    val crn = "X232323"
    setupNonCustodialSentence(crn)
    setupRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)
    calculateTierFor(crn)
    expectTierCalculation("A0")
  }

  @Test
  fun `calculate change for concurrent custodial and non-custodial sentence`() {
    val crn = "X676767"
    setupConcurrentCustodialAndNonCustodialSentence(crn)
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
    val crn = "X173878"
    setupTerminatedCustodialSentence(crn)
    setupMaleOffenderWithRegistrations(crn)
    calculateTierFor(crn)
    expectTierCalculation("A0")
  }

  @Test
  fun `calculate change for terminated non-custodial sentence and current non-custodial sentence with non-restrictive requirements`() {
    val crn = "X505050"
    setupCurrentNonCustodialSentenceAndTerminatedNonCustodialSentence(crn)
    setupNonRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)
    calculateTierFor(crn)
    expectTierCalculation("A1")
  }

  @Test
  fun `calculate change and protect for non-custodial sentence with non-restrictive requirements`() {
    val crn = "X222222"
    setupNonCustodialSentence(crn)
    setupNonRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)
    calculateTierFor(crn)
    expectTierCalculation("A1")
  }

  @Test
  fun `do not calculate change for terminated non-custodial sentence with non-restrictive requirements`() {
    val crn = "X888888"
    setupTerminatedNonCustodialSentence(crn)
    setupNonRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)
    calculateTierFor(crn)
    expectTierCalculation("A0")
  }

  @Test
  fun `do not calculate change when only restrictive requirements are present on a non-custodial sentence`() {
    val crn = "X888866"
    setupNonCustodialSentence(crn)
    setupRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)
    calculateTierFor(crn)
    expectTierCalculation("A0")
  }

  @Test
  fun `calculate change with restrictive and non-restrictive requirements on a non-custodial sentence`() {
    val crn = "X888855"
    setupNonCustodialSentence(crn)
    setupRestrictiveAndNonRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)
    calculateTierFor(crn)
    expectTierCalculation("A1")
  }

  @Test
  fun `do not calculate change when no requirements are present on a non-custodial sentence`() {
    val crn = "X888844"
    setupNonCustodialSentence(crn)
    setupNoRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)
    calculateTierFor(crn)
    expectTierCalculation("A0")
  }

  @Test
  fun `do not calculate change for non-custodial sentence where the only non-restrictive requirement is unpaid work`() {
    val crn = "X252525"
    setupNonCustodialSentence(crn)
    setupUnpaidWorkRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)
    calculateTierFor(crn)
    expectTierCalculation("A0")
  }

  @Test
  fun `do not calculate change for non-custodial sentence where the only non-restrictive requirements are unpaid work, order length extended and additional hours`() {
    val crn = "X252526"
    setupNonCustodialSentence(crn)
    setupUnpaidWorkWithOrderLengthExtendedAndAdditionalHoursRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)
    calculateTierFor(crn)
    expectTierCalculation("A0")
  }
}
