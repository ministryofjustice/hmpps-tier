package uk.gov.justice.digital.hmpps.hmppstier.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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

@RestController
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
class TierCalculationController(private val tierCalculationService: TierCalculationService) {

  @Operation(summary = "Retrieve tiering score by crn")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found")
    ]
  )

  @PreAuthorize("hasRole('ROLE_HMPPS_TIER')")
  @GetMapping("crn/{crn}/tier")
  fun getLatestTierCalculation(@PathVariable(required = true) crn: String): ResponseEntity<TierDto> = ResponseEntity.ok(tierCalculationService.getLatestTierByCrn(crn) ?: throw EntityNotFoundException("Tier Result Not Found for $crn"))

  @Operation(summary = "Retrieve tiering score by crn and calculation ID")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found")
    ]
  )
  @PreAuthorize("hasRole('ROLE_HMPPS_TIER')")
  @GetMapping("crn/{crn}/tier/{calculationId}")
  fun getTierCalculationById(@PathVariable(required = true) crn: String, @PathVariable(required = true) calculationId: UUID): ResponseEntity<TierDto> = ResponseEntity.ok(
    tierCalculationService.getTierByCalculationId(crn, calculationId)
      ?: throw EntityNotFoundException("Tier Result Not Found for $crn, $calculationId")
  )
}
