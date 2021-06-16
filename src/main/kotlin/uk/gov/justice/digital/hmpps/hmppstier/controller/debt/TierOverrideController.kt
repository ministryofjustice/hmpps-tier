package uk.gov.justice.digital.hmpps.hmppstier.controller.debt

import io.swagger.annotations.Api
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import uk.gov.justice.digital.hmpps.hmppstier.service.debt.TierOverrideService

@Api
@RestController
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
class TierOverrideController(private val tierOverrideService: TierOverrideService) {

  @PreAuthorize("hasRole('ROLE_HMPPS_TIER')")
  @PostMapping("/file")
  fun uploadCsvFile(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {
    tierOverrideService.uploadCsvFile(file)
    return ResponseEntity.ok().body("ok")
  }
}
