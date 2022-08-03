package uk.gov.justice.digital.hmpps.hmppstier.controller

import com.opencsv.bean.CsvBindByPosition
import com.opencsv.bean.CsvToBeanBuilder
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import uk.gov.justice.digital.hmpps.hmppstier.service.TriggerCalculationService
import java.io.InputStreamReader

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class TriggerTierCalculationController(private val triggerCalculationService: TriggerCalculationService) {

  @PostMapping("/crn/upload")
  fun uploadCrns(@RequestParam("file") file: MultipartFile): ResponseEntity<Void> {
    triggerCalculationService.sendEvents(fileToCases(file))
    return ResponseEntity.ok().build()
  }

  @Throws(Exception::class)
  fun fileToCases(file: MultipartFile): List<TriggerCsv> {
    val reader = InputStreamReader(file.inputStream)
    val cb = CsvToBeanBuilder<TriggerCsv>(reader)
      .withType(TriggerCsv::class.java)
      .build()
    val unallocatedCases = cb.parse()
    reader.close()
    return unallocatedCases
  }
}

data class TriggerCsv(
  @CsvBindByPosition(position = 0)
  var crn: String? = null
)
