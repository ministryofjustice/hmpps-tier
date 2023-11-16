package uk.gov.justice.digital.hmpps.hmppstier.controller

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppstier.service.TriggerCalculationService

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class TriggerTierCalculationController(private val triggerCalculationService: TriggerCalculationService) {

  @PostMapping("/crn/upload")
  suspend fun uploadCrns(@RequestPart("file") file: Mono<FilePart>): ResponseEntity<Void> {
    triggerCalculationService.sendEvents(fileToCases(file))
    return ResponseEntity.ok().build()
  }

  @PostMapping("/calculations")
  suspend fun recalculateTiers(@RequestBody(required = false) crns: List<String>?): Unit = coroutineScope {
    launch {
      if (crns.isNullOrEmpty()) {
        triggerCalculationService.recalculateAll()
      } else {
        crns.forEach { launch { triggerCalculationService.recalculate(it) } }
      }
    }
  }

  private suspend fun fileToCases(filePart: Mono<FilePart>): List<TriggerCsv> {
    return filePart.flatMapMany { file ->
      file.content().flatMapIterable { dataBuffer ->
        dataBuffer.asInputStream().bufferedReader().use { reader ->
          reader.lineSequence()
            .filter { it.isNotBlank() }
            .map { TriggerCsv(it) }
            .toList()
        }
      }
    }.asFlow().toList()
  }
}

data class TriggerCsv(
  var crn: String? = null,
)
