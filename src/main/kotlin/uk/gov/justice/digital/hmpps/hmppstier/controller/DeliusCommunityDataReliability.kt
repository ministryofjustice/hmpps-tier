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
) {

  @Operation(summary = "cross-check data between community API and Tier-To-Delius API")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_HMPPS_TIER')")
  @GetMapping("/crn/{crn}")
  suspend fun getDataReliability(@PathVariable(required = true) crn: String): CommunityDeliusData {
    val tierToDeliusResponse = tierToDeliusApiService.getTierToDelius(crn)
    val (rsrScoreCommunity, ogrsScoreCommunity) = communityApiService.getDeliusAssessments(crn)

    val rsrDelius = tierToDeliusResponse.rsrscore!!

    val ogrsDelius = tierToDeliusResponse.ogrsscore!!.div(10)
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

  @Operation(summary = "cross-check all crns between community API and Tier-To-Delius API")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_HMPPS_TIER')")
  @GetMapping("/crn/all")
  suspend fun getAllDataReliability(): List<CommunityDeliusData> {
    val crns = tierReader.getCrns().take(30)
    val communityDeliusDataList = mutableListOf<CommunityDeliusData>()

    crns.forEach {
      val tierToDeliusResponse = tierToDeliusApiService.getTierToDelius(it)
      val (rsrScoreCommunity, ogrsScoreCommunity) = communityApiService.getDeliusAssessments(it)

      val rsrDelius = tierToDeliusResponse.rsrscore!!

      val ogrsDelius = tierToDeliusResponse.ogrsscore!!.div(10)
      val ogrsCommunity = ogrsScoreCommunity.div(10)

      communityDeliusDataList.add(
        CommunityDeliusData(
          it,
          rsrDelius.compareTo(rsrScoreCommunity) == 0,
          ogrsDelius == ogrsCommunity,
          rsrDelius,
          rsrScoreCommunity,
          ogrsDelius,
          ogrsCommunity,
        ),
      )
    }
    return communityDeliusDataList
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
