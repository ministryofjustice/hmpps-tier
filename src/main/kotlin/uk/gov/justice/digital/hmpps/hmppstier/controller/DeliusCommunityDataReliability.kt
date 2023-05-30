package uk.gov.justice.digital.hmpps.hmppstier.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClientException
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.ConvictionDto
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusConviction
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusRegistration
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusRequirement
import uk.gov.justice.digital.hmpps.hmppstier.client.Nsi
import uk.gov.justice.digital.hmpps.hmppstier.client.Registration
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppstier.config.Generated
import uk.gov.justice.digital.hmpps.hmppstier.service.CommunityApiService
import uk.gov.justice.digital.hmpps.hmppstier.service.TierReader
import java.math.BigDecimal
@Generated
@RestController
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
class DeliusCommunityDataReliability(
  private val communityApiService: CommunityApiService,
  private val communityApiClient: CommunityApiClient,
  private val tierReader: TierReader,
  private val tierToDeliusApiClient: TierToDeliusApiClient,
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
  suspend fun getDataReliability(@PathVariable(required = true) crn: String): CommunityDeliusData? {
    val deliusInputs = tierToDeliusApiClient.getDeliusTierTest(crn)
    val (rsrScoreCommunity, ogrsScoreCommunity) = communityApiService.getDeliusAssessments(crn)

    val rsrDelius = deliusInputs?.rsrscore ?: BigDecimal.ZERO
    val ogrsDelius = (deliusInputs?.ogrsscore ?: 0).div(10)

    val genderMatch = communityApiService.getOffender(crn)?.gender.equals(deliusInputs?.gender, true)
    val ogrsCommunity = ogrsScoreCommunity.div(10)

    val communityConvictions = getCommunityConviction(crn)
    val deliusConvictions = deliusInputs?.convictions?.map { DeliusConviction(it.terminationDate, it.sentenceTypeCode, it.breached, it.requirements.sortedBy { it.mainCategoryTypeCode }) }
      ?.sortedWith(compareBy({ it.sentenceTypeCode }, { it.terminationDate }))

    val communityRegistrations = getCommunityRegistration(crn)
    val deliusRegistrations = deliusInputs?.registrations?.sortedWith(compareBy({ it.code }, { it.date }))

    return CommunityDeliusData(
      crn,
      rsrDelius.compareTo(rsrScoreCommunity) == 0,
      ogrsDelius == ogrsCommunity,
      genderMatch,
      communityConvictions == deliusConvictions,
      communityRegistrations == deliusRegistrations,
      rsrDelius,
      rsrScoreCommunity,
      ogrsDelius,
      ogrsCommunity,
      communityConvictions,
      deliusConvictions,
      communityRegistrations,
      deliusRegistrations,
    )
  }

  private suspend fun getCommunityRegistration(crn: String): List<DeliusRegistration> {
    return communityApiClient.getRegistrations(crn)?.map {
      DeliusRegistration(
        it.type.code,
        it.registerLevel?.code,
        it.startDate,
      )
    }?.sortedWith(compareBy({ it.code }, { it.date })) ?: emptyList()
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
          .map { DeliusRequirement(it.mainCategory, it.isRestrictive) }.sortedBy { it.mainCategoryTypeCode },
      )
    }.sortedWith(compareBy({ it.sentenceTypeCode }, { it.terminationDate }))
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
        null
      }
      val communityAssessment = try {
        communityApiService.getDeliusAssessments(it)
      } catch (e: WebClientException) {
        null
      }
      val rsrDelius = deliusInputs?.rsrscore ?: BigDecimal.ZERO
      val ogrsDelius = (deliusInputs?.ogrsscore ?: 0).div(10)
      val ogrsCommunity = communityAssessment?.ogrs?.div(10)
      val genderCommunity = communityApiService.getOffender(it)?.gender ?: "NOT_FOUND"

      val communityConvictions = getCommunityConviction(it)
      val deliusConvictions = deliusInputs?.convictions?.map { DeliusConviction(it.terminationDate, it.sentenceTypeCode, it.breached, it.requirements.sortedBy { it.mainCategoryTypeCode }) }
        ?.sortedWith(compareBy({ it.sentenceTypeCode }, { it.terminationDate }))
      val communityRegistrations = getCommunityRegistration(it)
      val deliusRegistrations = deliusInputs?.registrations?.sortedWith(compareBy({ it.code }, { it.date }))

      CommunityDeliusData(
        it,
        rsrDelius.compareTo(communityAssessment?.rsr) == 0,
        ogrsDelius == ogrsCommunity,
        genderCommunity.equals(deliusInputs?.gender, true),
        communityConvictions == deliusConvictions,
        communityRegistrations == deliusRegistrations,
        rsrDelius,
        communityAssessment?.rsr ?: BigDecimal.valueOf(-1),
        ogrsDelius,
        ogrsCommunity ?: -1,
        communityConvictions,
        deliusConvictions,
        communityRegistrations,
        deliusRegistrations,
      ).takeUnless {
        (deliusInputs == null) || (it.rsrMatch && it.ogrsMatch && it.convictionsMatch && it.registrationMatch)
      }
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
      .filter { tierToDeliusApiClient.getDeliusTierTest(it) == null }.toList()
  }

  @Operation(summary = "get community convictions")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_HMPPS_TIER')")
  @GetMapping("/community/convictions/{crn}")
  suspend fun getCommunityConvictions(@PathVariable(required = true) crn: String): List<ConvictionDto>? {
    return communityApiClient.getConvictions(crn)
  }

  @Operation(summary = "get community registration")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_HMPPS_TIER')")
  @GetMapping("/community/registrations/{crn}")
  suspend fun getCommunityRegistrations(@PathVariable(required = true) crn: String): List<Registration>? {
    return communityApiClient.getRegistrations(crn)?.toList()
  }

  @Operation(summary = "get community breach")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_HMPPS_TIER')")
  @GetMapping("/community/registrations/{crn}/{convictionId}")
  suspend fun getCommunityBreach(@PathVariable(required = true) crn: String, @PathVariable(required = true) convictionId: Long): List<Nsi>? {
    return communityApiClient.getBreachRecallNsis(crn, convictionId)
  }
}

@Generated
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
  val communityConvictions: List<DeliusConviction>,
  val deliusConvictions: List<DeliusConviction>?,
  val communityRegistrations: List<DeliusRegistration>,
  val deliusRegistrations: List<DeliusRegistration>?,
)
