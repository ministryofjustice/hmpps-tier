package uk.gov.justice.digital.hmpps.hmppstier.controller

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppstier.dto.TierDto
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService
import uk.gov.justice.digital.hmpps.hmppstier.service.exception.EntityNotFoundException
import java.util.UUID

@Api
@RestController
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
class TierCalculationController(private val tierCalculationService: TierCalculationService) {

  @ApiOperation(value = "Retrieve tiering score by crn")
  @ApiResponses(
    value = [
      ApiResponse(code = 200, message = "OK"),
      ApiResponse(code = 404, message = "Result Not Found")
    ]
  )

  @PreAuthorize("hasRole('ROLE_HMPPS_TIER')")
  @GetMapping("crn/{crn}/tier")
  fun getLatestTierCalculation(@PathVariable(required = true) crn: String): ResponseEntity<TierDto> = ResponseEntity.ok(tierCalculationService.getLatestTierByCrn(crn) ?: throw EntityNotFoundException("Tier Result Not Found for $crn"))

  @ApiOperation(value = "Retrieve tiering score by crn and calulation ID")
  @ApiResponses(
    value = [
      ApiResponse(code = 200, message = "OK"),
      ApiResponse(code = 404, message = "Result Not Found")
    ]
  )
  @PreAuthorize("hasRole('ROLE_HMPPS_TIER')")
  @GetMapping("crn/{crn}/tier/{calculationId}")
  fun getTierCalculationById(@PathVariable(required = true) crn: String, @PathVariable(required = true) calculationId: UUID): ResponseEntity<TierDto> = ResponseEntity.ok(
    tierCalculationService.getTierByCalculationId(crn, calculationId)
      ?: throw EntityNotFoundException("Tier Result Not Found for $crn, $calculationId")
  )
}
