package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.mockserver.model.HttpRequest.request
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppstier.controller.TierCalculationRequiredEventListener
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.emptyNsiResponse
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository

@TestInstance(PER_CLASS)
class TierCalculationTest : MockedEndpointsTestBase() {

  @Autowired
  lateinit var listener: TierCalculationRequiredEventListener

  @Autowired
  lateinit var repo: TierCalculationRepository

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

      listener.listen(calculationMessage(crn))

      val tier = repo.findFirstByCrnOrderByCreatedDesc(crn)

      Assertions.assertThat(tier?.data?.change?.tier).isEqualTo(ChangeLevel.TWO)
      Assertions.assertThat(tier?.data?.protect?.tier).isEqualTo(ProtectLevel.D)
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
    fun `default change to '2' for non recent assessment`() {
      val crn = "X432768"

      setupSCCustodialSentence(crn)
      setupMaleOffenderWithRegistrations(crn, includeAssessmentApi = false)
      setupLatestAssessment(crn, 2018)

      listener.listen(calculationMessage(crn))

      val tier = repo.findFirstByCrnOrderByCreatedDesc(crn)

      Assertions.assertThat(tier?.data?.change?.tier).isEqualTo(ChangeLevel.TWO)
      Assertions.assertThat(tier?.data?.protect?.tier).isEqualTo(ProtectLevel.A)
    }
  }
}
