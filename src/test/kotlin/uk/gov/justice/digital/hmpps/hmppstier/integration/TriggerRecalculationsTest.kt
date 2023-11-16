package uk.gov.justice.digital.hmpps.hmppstier.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.AssessmentApiExtension
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.tierToDeliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Registration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.TierDetails
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase

class TriggerRecalculationsTest : IntegrationTestBase() {

  @MockBean
  lateinit var telemetryClient: TelemetryClient

  @Test
  fun `providing crns recalculates only those crns`() {
    val crn = "T123456"
    val assessmentId = 5738261645L

    tierToDeliusApi.getFullDetails(
      crn,
      TierDetails(
        convictions = listOf(Conviction(sentenceCode = "SC")),
        registrations = listOf(
          Registration("M2"),
        ),
      ),
    )
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = assessmentId)

    restOfSetupWithMaleOffenderNoSevereNeeds(crn, false, assessmentId)
    AssessmentApiExtension.assessmentApi.getOutdatedAssessment(crn, assessmentId)

    webTestClient.post()
      .uri("/calculations")
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation())
      .bodyValue(listOf(crn))
      .exchange()
      .expectStatus().isOk

    verify(telemetryClient).trackEvent(
      "TierChanged",
      mapOf(
        "crn" to "T123456",
        "protect" to "A",
        "change" to "1",
        "version" to "2",
        "recalculationReason" to "CrnTrigger",
      ),
      null,
    )
  }

  @Test
  fun `providing no crns recalculates all active crns from delius`() {
    val crn = "D123456"
    val assessmentId = 67548387612L

    tierToDeliusApi.getCrns(listOf(crn))
    tierToDeliusApi.getFullDetails(
      crn,
      TierDetails(
        convictions = listOf(Conviction(sentenceCode = "SC")),
        registrations = listOf(
          Registration("M2"),
        ),
      ),
    )
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = assessmentId)

    restOfSetupWithMaleOffenderNoSevereNeeds(crn, false, assessmentId)
    AssessmentApiExtension.assessmentApi.getOutdatedAssessment(crn, assessmentId)

    webTestClient.post()
      .uri("/calculations")
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk

    verify(telemetryClient).trackEvent(
      "TierChanged",
      mapOf(
        "crn" to "D123456",
        "protect" to "A",
        "change" to "1",
        "version" to "2",
        "recalculationReason" to "FullRecalculationTrigger",
      ),
      null,
    )
  }
}
