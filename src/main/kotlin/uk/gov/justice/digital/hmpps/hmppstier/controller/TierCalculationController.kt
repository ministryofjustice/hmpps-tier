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
import uk.gov.justice.digital.hmpps.hmppstier.dto.TierDto
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCounts
import uk.gov.justice.digital.hmpps.hmppstier.service.TierReader
import uk.gov.justice.digital.hmpps.hmppstier.service.exception.EntityNotFoundException
import java.util.*

@RestController
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
class TierCalculationController(private val tierReader: TierReader) {

    @Operation(summary = "Retrieve number of cases for each tier")
    @PreAuthorize("hasRole('ROLE_HMPPS_TIER')")
    @GetMapping("tier-counts")
    fun getTierCounts(): List<TierCounts> = tierReader.getTierCounts()

    @Operation(summary = "Retrieve tiering score by crn")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "OK"),
            ApiResponse(responseCode = "404", description = "Result Not Found"),
        ],
    )
    @PreAuthorize("hasRole('ROLE_HMPPS_TIER')")
    @GetMapping("crn/{crn}/tier")
    fun getLatestTierCalculation(@PathVariable(required = true) crn: String): TierDto =
        tierReader.getLatestTierByCrn(crn) ?: throw EntityNotFoundException("Tier Result Not Found for $crn")

    @Operation(summary = "Retrieve tier history by crn")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "OK"),
            ApiResponse(responseCode = "404", description = "Result Not Found"),
        ],
    )
    @PreAuthorize("hasRole('ROLE_HMPPS_TIER')")
    @GetMapping("crn/{crn}/tier/history")
    fun getTierHistory(@PathVariable(required = true) crn: String): List<TierDto> =
        tierReader.getTierHistory(crn)

    @Operation(summary = "Retrieve latest tiering calculation details including inputs and scores")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "OK"),
            ApiResponse(responseCode = "404", description = "Result Not Found"),
        ],
    )
    @PreAuthorize("hasRole('ROLE_HMPPS_TIER')")
    @GetMapping("crn/{crn}/tier/details")
    fun getLatestTierCalculationDetails(@PathVariable(required = true) crn: String) =
        tierReader.getLatestTierDetailsByCrn(crn) ?: throw EntityNotFoundException("Tier Result Not Found for $crn")

    @Operation(summary = "Retrieve tiering score by crn and calculation ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "OK"),
            ApiResponse(responseCode = "404", description = "Result Not Found"),
        ],
    )
    @PreAuthorize("hasRole('ROLE_HMPPS_TIER')")
    @GetMapping("crn/{crn}/tier/{calculationId}")
    fun getTierCalculationById(
        @PathVariable(required = true) crn: String,
        @PathVariable(required = true) calculationId: UUID,
    ): TierDto = tierReader.getTierByCalculationId(crn, calculationId)
        ?: throw EntityNotFoundException("Tier Result Not Found for $crn, $calculationId")
}
