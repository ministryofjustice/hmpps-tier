package uk.gov.justice.digital.hmpps.hmppstier.controller

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import uk.gov.justice.digital.hmpps.hmppstier.service.TriggerCalculationService

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
    return file.inputStream.bufferedReader().use { reader ->
      reader.lineSequence()
        .filter { it.isNotBlank() }
        .map { TriggerCsv(it) }.toList()
    }
  }
}

data class TriggerCsv(
  var crn: String? = null,
)
