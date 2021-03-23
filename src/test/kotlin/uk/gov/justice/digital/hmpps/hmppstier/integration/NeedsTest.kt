package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppstier.controller.TierCalculationRequiredEventListener
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository

@TestInstance(PER_CLASS)
class NeedsTest : MockedEndpointsTestBase() {

  @Autowired
  lateinit var listener: TierCalculationRequiredEventListener

  @Autowired
  lateinit var repo: TierCalculationRepository

  @Test
  fun `severe needs 18 points plus 2 OGRS make change level 3`() {
    val crn = "X333444"
    setupSCCustodialSentence(crn)
    setupRegistrations(ApiResponses.registrationsResponse(), crn)
    restOfSetupWithMaleOffenderAndSevereNeeds(crn)
    listener.listen(calculationMessage(crn))

    val tier = repo.findFirstByCrnOrderByCreatedDesc(crn)

    Assertions.assertThat(tier?.data?.change?.tier).isEqualTo(ChangeLevel.THREE)
    Assertions.assertThat(tier?.data?.protect?.tier).isEqualTo(ProtectLevel.A)
  }
}
