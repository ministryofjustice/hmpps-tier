package uk.gov.justice.digital.hmpps.hmppstier.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusResponse
import uk.gov.justice.digital.hmpps.hmppstier.service.TierToDeliusApiService

@RestController
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
class TierToDeliusController(private val tierToDeliusApiService: TierToDeliusApiService) {

  @Operation(summary = "Retrieve tiering details by crn - TEST")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_TIER_DETAILS')")
  @GetMapping("/tier-details-test/{crn}")
  suspend fun getTierToDelius(@PathVariable(required = true) crn: String): TierToDeliusResponse {
    return tierToDeliusApiService.getTierToDelius(crn)
  }
}
