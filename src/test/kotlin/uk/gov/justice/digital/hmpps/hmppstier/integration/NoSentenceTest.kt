package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.mockserver.model.HttpRequest.request
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppstier.controller.TierCalculationRequiredEventListener
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.noSentenceConvictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.registrationsResponse
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository

@TestInstance(PER_CLASS)
class NoSentenceTest : MockedEndpointsTestBase() {

  @Autowired
  lateinit var listener: TierCalculationRequiredEventListener

  @Autowired
  lateinit var repo: TierCalculationRepository

  @Test
  fun `Tier is calculated with change level zero when no sentence is found`() {
    val crn = "X333444"
    setUpNoSentence(crn)
    setupRegistrations(registrationsResponse(), crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn)
    listener.listen(calculationMessage(crn))

    val tier = repo.findFirstByCrnOrderByCreatedDesc(crn)

    Assertions.assertThat(tier?.data?.change?.tier).isEqualTo(ChangeLevel.ZERO)
    Assertions.assertThat(tier?.data?.protect?.tier).isEqualTo(ProtectLevel.A)
  }

  private fun setUpNoSentence(crn: String) {
    mockCommunityApiServer.`when`(request().withPath("/secure/offenders/crn/$crn/convictions")).respond(
      jsonResponseOf(noSentenceConvictionResponse())
    )
  }
}
