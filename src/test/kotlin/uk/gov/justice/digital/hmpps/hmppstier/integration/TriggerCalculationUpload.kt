package uk.gov.justice.digital.hmpps.hmppstier.integration

import com.opencsv.CSVWriter
import com.opencsv.bean.StatefulBeanToCsv
import com.opencsv.bean.StatefulBeanToCsvBuilder
import org.junit.jupiter.api.Test
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppstier.controller.TriggerCsv
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.MockedEndpointsTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithMappa
import java.io.File
import java.io.FileWriter

class TriggerCalculationUpload : MockedEndpointsTestBase() {

  @Test
  fun `trigger a tier calculation from upload`() {
    val crn = "X546739"

    setupSCCustodialSentence(crn)
    setupRegistrations(registrationsResponseWithMappa(), crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, false, "4234568890")
    setupOutdatedAssessment(crn, "1234567890")

    webTestClient.post()
      .uri("/crn/upload")
      .contentType(MediaType.MULTIPART_FORM_DATA)
      .body(generateMultipartBody(crn))
      .exchange()
      .expectStatus()
      .isOk

    expectTierChangedById("A2")
  }

  @Test
  fun `must not write back even if tier is unchanged`() {
    val crn = "X432769"

    setupSCCustodialSentence(crn)
    setupRegistrations(registrationsResponseWithMappa(), crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, false, "4234568890")
    setupOutdatedAssessment(crn, "1234567890")

    calculateTierFor(crn)
    expectTierChangedById("A2")

    setupSCCustodialSentence(crn)
    setupMaleOffenderWithRegistrations(crn, false, "4234568890")
    setupOutdatedAssessment(crn, "1234567890")

    webTestClient.post()
      .uri("/crn/upload")
      .contentType(MediaType.MULTIPART_FORM_DATA)
      .body(generateMultipartBody(crn))
      .exchange()
      .expectStatus()
      .isOk

    expectNoUpdatedTierCalculation()
  }

  private fun generateMultipartBody(crn: String): BodyInserters.MultipartInserter {
    val cases = listOf(TriggerCsv(crn))
    val csvFile = generateCsv(cases)
    val multipartBodyBuilder = MultipartBodyBuilder()
    multipartBodyBuilder.part("file", FileSystemResource(csvFile))
    return BodyInserters.fromMultipartData(multipartBodyBuilder.build())
  }

  fun generateCsv(unallocatedCases: List<TriggerCsv>): File {
    val tempFile = kotlin.io.path.createTempFile().toFile()
    val writer = FileWriter(tempFile)

    val sbc: StatefulBeanToCsv<TriggerCsv> = StatefulBeanToCsvBuilder<TriggerCsv>(writer)
      .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
      .build()
    sbc.write(unallocatedCases)
    writer.close()
    return tempFile
  }
}
