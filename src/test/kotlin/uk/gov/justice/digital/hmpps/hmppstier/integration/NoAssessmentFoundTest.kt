package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppstier.controller.TierCalculationRequiredEventListener
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel.TWO
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel.A
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.registrationsResponse
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository

@TestInstance(PER_CLASS)
class NoAssessmentFoundTest : MockedEndpointsTestBase() {

  @Autowired
  lateinit var listener: TierCalculationRequiredEventListener

  @Autowired
  lateinit var repo: TierCalculationRepository

  @Test
  fun `changeLevel should be 2 if assessment returns 404`() {
    val crn = "X373878"
    setupNCCustodialSentence(crn)
    setupAssessmentNotFound(crn)
    setupRegistrations(registrationsResponse(), crn)
    restOfSetupWithMaleOffender(crn)
    setupUpdateTierSuccess(crn, "A2")

    listener.listen(calculationMessage(crn))
    val tier = repo.findFirstByCrnOrderByCreatedDesc(crn)

    assertThat(tier?.data?.change?.tier).isEqualTo(TWO)
    assertThat(tier?.data?.protect?.tier).isEqualTo(A)
  }
}
