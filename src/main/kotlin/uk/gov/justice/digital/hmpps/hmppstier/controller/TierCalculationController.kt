package uk.gov.justice.digital.hmpps.hmppstier.controller

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppstier.dto.TierDto
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService

@Api
@RestController
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
class TierCalculationController(private val tierCalculationService: TierCalculationService) {

  @ApiOperation(value = "Retrieve tiering score by crn")
  @ApiResponses(
    value = [
      ApiResponse(code = 200, message = "OK", response = TierDto::class)
    ]
  )
  @GetMapping("crn/{crn}/tier")
  fun getNotifications(@PathVariable(required = true) crn: String): ResponseEntity<TierDto> {
    return ResponseEntity.ok(tierCalculationService.getTierByCrn(crn))
  }
}
