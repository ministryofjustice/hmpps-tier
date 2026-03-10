package uk.gov.justice.digital.hmpps.hmppstier.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "V2")
@PreAuthorize("hasRole('HMPPS_TIER')")
@RequestMapping("v2", produces = [APPLICATION_JSON_VALUE])
class TierV2Controller {

}
