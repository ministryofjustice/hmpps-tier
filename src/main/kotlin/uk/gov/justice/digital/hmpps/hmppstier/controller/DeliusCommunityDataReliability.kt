package uk.gov.justice.digital.hmpps.hmppstier.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClientException
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusConviction
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusRegistration
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusRequirement
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusAssessments
import uk.gov.justice.digital.hmpps.hmppstier.service.CommunityApiService
import uk.gov.justice.digital.hmpps.hmppstier.service.TierReader
import java.math.BigDecimal

@RestController
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
class DeliusCommunityDataReliability(
  private val communityApiService: CommunityApiService,
  private val communityApiClient: CommunityApiClient,
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
    val deliusInputs = tierToDeliusApiClient.getDeliusTierTest(crn)
    val (rsrScoreCommunity, ogrsScoreCommunity) = communityApiService.getDeliusAssessments(crn)

    val rsrDelius = deliusInputs?.rsrscore ?: BigDecimal.ZERO
    val ogrsDelius = (deliusInputs?.ogrsscore ?: 0).div(10)

    val genderMatch = communityApiService.getOffender(crn)?.gender.equals(deliusInputs?.gender, true)
    val ogrsCommunity = ogrsScoreCommunity.div(10)

    return CommunityDeliusData(
      crn,
      rsrDelius.compareTo(rsrScoreCommunity) == 0,
      ogrsDelius == ogrsCommunity,
      genderMatch,
      getCommunityConviction(crn) == deliusInputs?.convictions,
      getCommunityRegistration(crn) == deliusInputs?.registrations,
      rsrDelius,
      rsrScoreCommunity,
      ogrsDelius,
      ogrsCommunity,
    )
  }

  private suspend fun getCommunityRegistration(crn: String): List<DeliusRegistration> {
    return communityApiClient.getRegistrations(crn).map {
      DeliusRegistration(
        it.type.code,
        it.registerLevel?.code,
        it.startDate,
      )
    }
  }

  private suspend fun getCommunityConviction(crn: String):
    List<DeliusConviction> {
    val communityConvictions = communityApiService.getConvictionsWithSentences(crn)
    return communityConvictions.map {
      DeliusConviction(
        it.sentence.terminationDate,
        it.sentence.sentenceType,
        communityApiService.hasBreachedConvictions(crn, communityConvictions),
        communityApiService.getRequirements(crn, it.convictionId)
          .map { DeliusRequirement(it.mainCategory, it.isRestrictive) },
      )
    }
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
      val rsrDelius = deliusInputs?.rsrscore ?: BigDecimal.ZERO
      val ogrsDelius = (deliusInputs?.ogrsscore ?: 0).div(10)
      val ogrsCommunity = communityAssessment.ogrs.div(10)
      val genderMatch = communityApiService.getOffender(it)?.gender.equals(deliusInputs?.gender, true)

      CommunityDeliusData(
        it,
        rsrDelius?.compareTo(communityAssessment.rsr) == 0,
        ogrsDelius == ogrsCommunity,
        genderMatch,
        getCommunityConviction(it) == deliusInputs?.convictions,
        getCommunityRegistration(it) == deliusInputs?.registrations,
        rsrDelius,
        communityAssessment.rsr,
        ogrsDelius,
        ogrsCommunity,
      ).takeUnless { deliusInputs?.ogrsscore == -1 || (it.rsrMatch && it.ogrsMatch) }
    }
  }

  @Operation(summary = "get CRNs not available in Tier-To-Delius API")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_HMPPS_TIER')")
  @GetMapping("/crn/{offset}/{limit}")
  suspend fun getUnmatchedCrns(@PathVariable(required = true) offset: Int, @PathVariable(required = true) limit: Int): List<String>? {
    return tierReader.getCrns(offset, limit)
      .filter { tierToDeliusApiClient.getDeliusTierTest(it)?.ogrsscore == -1 }.toList()
  }
}

data class CommunityDeliusData(
  val crn: String,
  val rsrMatch: Boolean,
  val ogrsMatch: Boolean,
  val genderMatch: Boolean,
  val convictionsMatch: Boolean,
  val registrationMatch: Boolean,
  val rsrDelius: BigDecimal,
  val rsrCommunity: BigDecimal,
  val ogrsDelius: Int?,
  val ogrsCommunity: Int,
)
