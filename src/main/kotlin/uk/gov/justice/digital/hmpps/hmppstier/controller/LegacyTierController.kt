package uk.gov.justice.digital.hmpps.hmppstier.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppstier.service.TierV2Reader

@RestController
@Tag(name = "Deprecated")
@Deprecated("Use 'v2' or 'v3' path")
@PreAuthorize("hasRole('HMPPS_TIER')")
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
class LegacyTierController(tierReader: TierV2Reader) : TierV2Controller(tierReader)