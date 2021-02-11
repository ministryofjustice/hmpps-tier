package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.emptyRegistrationsResponse
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationRequiredEventListener
import java.nio.file.Files
import java.nio.file.Paths

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MissingRegistrationTest : MockedEndpointsTestBase() {

  @Autowired
  lateinit var listener: TierCalculationRequiredEventListener

  @Autowired
  lateinit var repo: TierCalculationRepository

  @Test
  fun `calculate change and protect when no registrations are found`() {
    val crn = "X373878"
    setupNCCustodialSentence(crn)
    setupRegistrations(emptyRegistrationsResponse(), crn)
    restOfSetup(crn)
    val validMessage: String =
      Files.readString(Paths.get("src/test/resources/fixtures/sqs/tier-calculation-event.json"))
    listener.listen(validMessage)
    val tier = repo.findFirstByCrnOrderByCreatedDesc(crn)

    Assertions.assertThat(tier?.data?.change?.tier).isEqualTo(ChangeLevel.ONE)
    Assertions.assertThat(tier?.data?.protect?.tier).isEqualTo(ProtectLevel.B)
  }

  private fun restOfSetup(crn: String) {
    mockCommunityApiServer.`when`(HttpRequest.request().withPath("/offenders/crn/$crn/assessments")).respond(
      HttpResponse.response().withContentType(
        MediaType.APPLICATION_JSON
      ).withBody(ApiResponses.communityApiAssessmentsResponse())
    )

    mockCommunityApiServer.`when`(HttpRequest.request().withPath("/offenders/crn/$crn")).respond(
      HttpResponse.response().withContentType(
        MediaType.APPLICATION_JSON
      ).withBody(ApiResponses.offenderResponse())
    )
    mockAssessmentApiServer.`when`(
      HttpRequest.request().withPath("/offenders/crn/$crn/assessments/latest"),
      Times.exactly(2)
    )
      .respond(
        HttpResponse.response().withContentType(
          MediaType.APPLICATION_JSON
        ).withBody(ApiResponses.assessmentsApiAssessmentsResponse())
      )
    mockAssessmentApiServer.`when`(HttpRequest.request().withPath("/assessments/oasysSetId/1234/needs")).respond(
      HttpResponse.response().withContentType(
        MediaType.APPLICATION_JSON
      ).withBody(ApiResponses.assessmentsApiNeedsResponse())
    )
  }
}
