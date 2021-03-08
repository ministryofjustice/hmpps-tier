package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppstier.controller.TierCalculationRequiredEventListener

@TestInstance(PER_CLASS)
class NeedsTest : MockedEndpointsTestBase() {

  @Autowired
  lateinit var listener: TierCalculationRequiredEventListener

  @Test
  fun `severe needs 18 points plus 2 OGRS make change level 3`() {
    val crn = "X333444"
    setupSCCustodialSentence(crn)
    setupRegistrations(ApiResponses.registrationsResponse(), crn)
    restOfSetupWithMaleOffenderAndSevereNeeds(crn)
    tierUpdateWillSucceed(crn, "A3")
    listener.listen(calculationMessage(crn))
  }
}
