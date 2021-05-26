package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.MockedEndpointsTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithMappa
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithRosh
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithRoshMappaAndAdditionalFactors

class ProtectTierATest : MockedEndpointsTestBase() {

  @BeforeAll
  fun `Enable Version 2`() {
    super.calculationVersionHelper.calculationVersion = 2
  }

  @Test
  fun `Tier is A with Mappa M2`() {
    val crn = "X333477"
    setupNCCustodialSentence(crn)
    setupRegistrations(registrationsResponseWithMappa("M2"), crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = "5234567890")
    calculateTierFor(crn)
    expectTierCalculation("A1")
  }

  @Test
  fun `Tier is A with Mappa M3`() {
    val crn = "X333478"
    setupNCCustodialSentence(crn)
    setupRegistrations(registrationsResponseWithMappa("M3"), crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = "5234567890")
    calculateTierFor(crn)
    expectTierCalculation("A1")
  }

  @Test
  fun `Tier is A with ROSH VERY HIGH`() {
    val crn = "X333479"
    setupNCCustodialSentence(crn)
    setupRegistrations(registrationsResponseWithRosh(Rosh.VERY_HIGH.registerCode), crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = "5234567890")
    calculateTierFor(crn)
    expectTierCalculation("A1")
  }

  @Test
  fun `Tier is B with low Mappa but 30 points`() {
    val crn = "X333480"
    setupNCCustodialSentence(crn)
    setupRegistrations(registrationsResponseWithRoshMappaAndAdditionalFactors(Rosh.HIGH.registerCode, "M1", listOf("RCCO", "RCPR", "RCHD")), crn)
    restOfSetupWithMaleOffenderAndSevereNeeds(crn, assessmentId = "5234567890")
    calculateTierFor(crn)
    expectTierCalculation("B1")
  }
}
