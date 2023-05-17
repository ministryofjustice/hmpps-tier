package uk.gov.justice.digital.hmpps.hmppstier.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClientException
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusAssessments
import uk.gov.justice.digital.hmpps.hmppstier.service.CommunityApiService
import uk.gov.justice.digital.hmpps.hmppstier.service.TierReader
import uk.gov.justice.digital.hmpps.hmppstier.service.TierToDeliusApiService
import java.math.BigDecimal

@RestController
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
class DeliusCommunityDataReliability(
  private val communityApiService: CommunityApiService,
  private val tierToDeliusApiService: TierToDeliusApiService,
  private val tierReader: TierReader,
  private val tierToDeliusApiClient: TierToDeliusApiClient,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Operation(summary = "cross-check data between community API and Tier-To-Delius API")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_HMPPS_TIER')")
  @GetMapping("/crn/{crn}")
  suspend fun getDataReliability(@PathVariable(required = true) crn: String): CommunityDeliusData? {
    val deliusInputs = tierToDeliusApiService.getTierToDelius(crn)
    val (rsrScoreCommunity, ogrsScoreCommunity) = communityApiService.getDeliusAssessments(crn)

    val rsrDelius = deliusInputs.rsrScore

    val ogrsDelius = deliusInputs.ogrsScore.div(10)
    val ogrsCommunity = ogrsScoreCommunity.div(10)

    return CommunityDeliusData(
      crn,
      rsrDelius.compareTo(rsrScoreCommunity) == 0,
      ogrsDelius == ogrsCommunity,
      rsrDelius,
      rsrScoreCommunity,
      ogrsDelius,
      ogrsCommunity,
    )
  }

  @Operation(summary = "find discrepancy between community API and Tier-To-Delius API for Tiering CRNs")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_HMPPS_TIER')")
  @GetMapping("/crn/all/{offset}/{limit}")
  suspend fun getAllDataReliability(@PathVariable(required = true) offset: Int, @PathVariable(required = true) limit: Int): Flow<CommunityDeliusData> {
    return tierReader.getCrns(offset, limit).mapNotNull {
      val deliusInputs = try {
        tierToDeliusApiClient.getDeliusTierTest(it)
      } catch (e: WebClientException) {
        log.error("Webclient exception in Tier To Delius for CRN: $it", e)
        null
      }
      val communityAssessment = try {
        communityApiService.getDeliusAssessments(it)
      } catch (e: WebClientException) {
        log.error("Webclient exception in Community API for CRN: $it", e)
        DeliusAssessments(BigDecimal.valueOf(-1), -10)
      }
      val rsrDelius = deliusInputs?.rsrscore
      val ogrsDelius = deliusInputs?.ogrsscore?.div(10)
      val ogrsCommunity = communityAssessment.ogrs.div(10)

      CommunityDeliusData(
        it,
        rsrDelius?.compareTo(communityAssessment.rsr) == 0,
        ogrsDelius == ogrsCommunity,
        rsrDelius ?: BigDecimal.ZERO,
        communityAssessment.rsr,
        ogrsDelius,
        ogrsCommunity,
      ).takeUnless { deliusInputs == null || (it.rsrMatch && it.ogrsMatch) }
    }
  }
}

data class CommunityDeliusData(
  val crn: String,
  val rsrMatch: Boolean,
  val ogrsMatch: Boolean,
  val rsrDelius: BigDecimal,
  val rsrCommunity: BigDecimal,
  val ogrsDelius: Int?,
  val ogrsCommunity: Int,
)
