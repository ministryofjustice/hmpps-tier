package uk.gov.justice.digital.hmpps.hmppstier.controller

import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import uk.gov.justice.digital.hmpps.hmppstier.service.TriggerCalculationService

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class TriggerTierCalculationController(private val triggerCalculationService: TriggerCalculationService) {

    @PreAuthorize("hasRole('ROLE_MANAGEMENT_TIER_UPDATE')")
    @PostMapping("/calculations")
    fun recalculateTiers(
        @RequestBody(required = false) crns: Set<String>?,
        @RequestParam(required = false, defaultValue = "true") dryRun: Boolean
    ) {
        Thread.ofVirtual().start {
            if (crns.isNullOrEmpty()) {
                triggerCalculationService.recalculateAll(dryRun)
            } else {
                triggerCalculationService.recalculate(crns, dryRun)
            }
        }
    }
}
