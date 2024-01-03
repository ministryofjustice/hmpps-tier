package uk.gov.justice.digital.hmpps.hmppstier.controller

import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppstier.service.TriggerCalculationService

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class TriggerTierCalculationController(private val triggerCalculationService: TriggerCalculationService) {

  @PreAuthorize("hasRole('ROLE_MANAGEMENT_TIER_UPDATE')")
  @PostMapping("/calculations")
  fun recalculateTiers(@RequestBody(required = false) crns: List<String>?) {
    Thread.ofVirtual().start {
      if (crns.isNullOrEmpty()) {
        triggerCalculationService.recalculateAll()
      } else {
        triggerCalculationService.recalculate(crns)
      }
    }
  }
}

data class TriggerCsv(
  var crn: String? = null,
)
