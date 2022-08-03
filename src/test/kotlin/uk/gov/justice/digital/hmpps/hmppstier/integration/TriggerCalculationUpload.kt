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
    val crn = "X432769"

    setupSCCustodialSentence(crn)
    setupRegistrations(registrationsResponseWithMappa(), crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, false, "4234568890")
    setupOutdatedAssessment(crn, "1234567890")

    val cases = listOf(TriggerCsv(crn))
    val csvFile = generateCsv(cases)
    val multipartBodyBuilder = MultipartBodyBuilder()
    multipartBodyBuilder.part("file", FileSystemResource(csvFile))

    webTestClient.post()
      .uri("/crn/upload")
      .contentType(MediaType.MULTIPART_FORM_DATA)
      .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
      .exchange()
      .expectStatus()
      .isOk

    expectTierCalculationById("A2")
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
