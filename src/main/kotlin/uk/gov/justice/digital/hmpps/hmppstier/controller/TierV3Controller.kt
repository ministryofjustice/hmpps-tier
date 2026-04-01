package uk.gov.justice.digital.hmpps.hmppstier.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppstier.exception.EntityNotFoundException
import uk.gov.justice.digital.hmpps.hmppstier.model.TierDto
import uk.gov.justice.digital.hmpps.hmppstier.service.TierV3Reader
import java.util.*

@RestController
@Tag(name = "V3")
@RequestMapping("v3", produces = [APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('HMPPS_TIER', 'PROBATION_INTEGRATION_ADMIN')")
class TierV3Controller(private val tierReader: TierV3Reader) {
    @Operation(summary = "Retrieve number of cases for each tier")
    @GetMapping("tier-counts")
    fun getTierCounts() = tierReader.getTierCounts()

    @Operation(summary = "Retrieve tiering score by crn")
    @GetMapping("crn/{crn}/tier")
    fun getLatestTierCalculation(@PathVariable(required = true) crn: String): TierDto =
        tierReader.getLatestTierByCrn(crn) ?: throw EntityNotFoundException("Tier Result Not Found for $crn")

    @Operation(summary = "Retrieve tier history by crn")
    @GetMapping("crn/{crn}/tier/history")
    fun getTierHistory(@PathVariable(required = true) crn: String): List<TierDto> =
        tierReader.getTierHistory(crn)

    @Operation(summary = "Retrieve latest tiering calculation details including inputs and scores")
    @GetMapping("crn/{crn}/tier/details")
    fun getLatestTierCalculationDetails(@PathVariable(required = true) crn: String) =
        tierReader.getLatestTierDetailsByCrn(crn) ?: throw EntityNotFoundException("Tier Result Not Found for $crn")

    @Operation(summary = "Retrieve tiering score by crn and calculation ID")
    @GetMapping("crn/{crn}/tier/{calculationId}")
    fun getTierCalculationById(
        @PathVariable(required = true) crn: String,
        @PathVariable(required = true) calculationId: UUID,
    ): TierDto = tierReader.getTierByCalculationId(crn, calculationId)
        ?: throw EntityNotFoundException("Tier Result Not Found for $crn, $calculationId")
}
