package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType.MULTIPART_FORM_DATA
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppstier.controller.TriggerCsv
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.AssessmentApiExtension.Companion.assessmentApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.CommunityApiExtension.Companion.communityApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.domain.Sentence
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.tierToDeliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.TierDetails
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import java.io.File

class TriggerCalculationUpload : IntegrationTestBase() {

  @Test
  fun `trigger a tier calculation from upload`() {
    val crn = "X546739"
    tierToDeliusApi.getFullDetails(crn, TierDetails(convictions = listOf(Conviction(sentence = Sentence(sentenceCode = "SC")))))
    communityApi.getMappaRegistration(crn, "M2")
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, false, 4234568890)
    assessmentApi.getOutdatedAssessment(crn, 1234567890)

    webTestClient.post()
      .uri("/crn/upload")
      .contentType(MULTIPART_FORM_DATA)
      .body(generateMultipartBody(crn))
      .exchange()
      .expectStatus()
      .isOk
    expectTierChangedById("A2")
  }

  @Test
  fun `do not trigger a calculation for blank rows`() {
    val crn = "X546739"
    tierToDeliusApi.getFullDetails(crn, TierDetails(convictions = listOf(Conviction(sentence = Sentence(sentenceCode = "SC")))))
    communityApi.getMappaRegistration(crn, "M2")
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, false, 4234568890)
    assessmentApi.getOutdatedAssessment(crn, 1234567890)

    webTestClient.post()
      .uri("/crn/upload")
      .contentType(MULTIPART_FORM_DATA)
      .body(generateMultipartBody("", crn))
      .exchange()
      .expectStatus()
      .isOk

    expectTierChangedById("A2")
    expectNoMessagesOnQueueOrDeadLetterQueue()
  }

  @Test
  fun `must not write back if tier is unchanged`() {
    val crn = "X432769"
    tierToDeliusApi.getFullDetails(crn, TierDetails(convictions = listOf(Conviction(sentence = Sentence(sentenceCode = "SC")))))
    communityApi.getMappaRegistration(crn, "M2")
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, false, 4234568890)
    assessmentApi.getOutdatedAssessment(crn, 1234567890)

    calculateTierFor(crn)
    expectTierChangedById("A2")

    tierToDeliusApi.getFullDetails(crn, TierDetails(convictions = listOf(Conviction(sentence = Sentence(sentenceCode = "SC")))))
    communityApi.getMappaRegistration(crn, "M2")
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, false, 4234568890, "A2")
    assessmentApi.getOutdatedAssessment(crn, 1234567890)

    webTestClient.post()
      .uri("/crn/upload")
      .contentType(MULTIPART_FORM_DATA)
      .body(generateMultipartBody(crn))
      .exchange()
      .expectStatus()
      .isOk

    expectNoUpdatedTierCalculation()
  }

  private fun generateMultipartBody(crn1: String, crn2: String = ""): BodyInserters.MultipartInserter {
    val cases = listOf(TriggerCsv(crn1), TriggerCsv(crn2))
    val csvFile = generateCsv(cases)
    val multipartBodyBuilder = MultipartBodyBuilder()
    multipartBodyBuilder.part("file", FileSystemResource(csvFile))
    return BodyInserters.fromMultipartData(multipartBodyBuilder.build())
  }

  fun generateCsv(unallocatedCases: List<TriggerCsv>): File {
    val tempFile = kotlin.io.path.createTempFile().toFile()
    tempFile.printWriter().use { out ->
      unallocatedCases.forEach {
        out.println(it.crn)
      }
    }
    return tempFile
  }
}
