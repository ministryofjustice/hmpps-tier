package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor.IOM_NOMINAL
import java.math.BigDecimal
import java.time.LocalDate

@Component
class CommunityApiClient(@Qualifier("communityWebClientAppScope") private val webClient: WebClient) {

  fun getRegistrations(crn: String): Pair<List<Registration>, List<Registration>> =
    getRegistrationsCall(crn).sortedByDescending { it.startDate }.partition { it.type.code == IOM_NOMINAL.registerCode }

  fun getDeliusAssessments(crn: String): DeliusAssessmentsDto? {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/assessments")
      .retrieve()
      .bodyToMono(DeliusAssessmentsDto::class.java)
      .block()
  }

  fun getConvictions(crn: String): List<ConvictionDto> {
    val responseType = object : ParameterizedTypeReference<List<ConvictionDto>>() {}
    return webClient
      .get()
      .uri("/offenders/crn/$crn/convictions?activeOnly=true")
      .retrieve()
      .bodyToMono(responseType)
      .block() ?: listOf()
  }

  fun getBreachRecallNsis(crn: String, convictionId: Long): List<Nsi> {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/convictions/$convictionId/nsis?nsiCodes=BRE,BRES,REC,RECS")
      .retrieve()
      .bodyToMono(NsiWrapper::class.java)
      .block()?.nsis ?: listOf()
  }

  fun getOffender(crn: String): Offender? {
    return webClient
      .get()
      .uri("/offenders/crn/$crn")
      .retrieve()
      .bodyToMono(Offender::class.java)
      .block()
  }

  fun getRequirements(crn: String, convictionId: Long): List<Requirement> {
    return getRequirementsCall(crn, convictionId)
      .filterNot { it.requirementTypeMainCategory == null && it.restrictive == null }
      .map { Requirement(it.restrictive!!, it.requirementTypeMainCategory!!.code) }
  }

  private fun getRegistrationsCall(crn: String): Collection<Registration> {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/registrations?activeOnly=true")
      .retrieve()
      .bodyToMono(Registrations::class.java)
      .block()?.registrations ?: listOf()
  }

  private fun getRequirementsCall(crn: String, convictionId: Long): List<RequirementDto> {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/convictions/$convictionId/requirements?activeOnly=true")
      .retrieve()
      .bodyToMono(Requirements::class.java)
      .block()?.requirements ?: listOf()
  }
}

private data class Requirements @JsonCreator constructor(
  @JsonProperty("requirements")
  val requirements: List<RequirementDto>
)

private data class RequirementDto @JsonCreator constructor(
  @JsonProperty("restrictive")
  val restrictive: Boolean?,

  @JsonProperty("requirementTypeMainCategory")
  val requirementTypeMainCategory: RequirementTypeMainCategory?
)

data class Requirement @JsonCreator constructor(
  val isRestrictive: Boolean,

  val mainCategory: String
)

private data class RequirementTypeMainCategory @JsonCreator constructor(
  @JsonProperty("code")
  val code: String
)

private data class NsiWrapper @JsonCreator constructor(
  @JsonProperty("nsis")
  val nsis: List<Nsi>,
)

data class Nsi @JsonCreator constructor(
  @JsonProperty("nsiOutcome")
  val status: KeyValue?
)

data class ConvictionDto @JsonCreator constructor(
  @JsonProperty("convictionId")
  val convictionId: Long,

  @JsonProperty("sentence")
  val sentence: SentenceDto?,

  @JsonProperty("offences")
  val offences: List<Offence?>,

  @JsonProperty("latestCourtAppearanceOutcome")
  val latestCourtAppearanceOutcome: KeyValue?

)

data class SentenceDto @JsonCreator constructor(
  @JsonProperty("terminationDate")
  val terminationDate: LocalDate?,

  @JsonProperty("sentenceType")
  val sentenceType: KeyValue,

  @JsonProperty("startDate")
  val startDate: LocalDate,

  @JsonProperty("expectedSentenceEndDate")
  val expectedSentenceEndDate: LocalDate?,
)

data class Offence @JsonCreator constructor(
  @JsonProperty("detail")
  val offenceDetail: OffenceDetail
)

data class OffenceDetail @JsonCreator constructor(
  @JsonProperty("mainCategoryCode")
  val mainCategoryCode: String
)

data class Offender @JsonCreator constructor(
  @JsonProperty("gender")
  val gender: String?,
)

data class DeliusAssessmentsDto @JsonCreator constructor(
  @JsonProperty("rsrScore")
  val rsr: BigDecimal?,
  @JsonProperty("ogrsScore")
  val ogrs: Int?
)

data class KeyValue @JsonCreator constructor(
  @JsonProperty("code")
  val code: String
)

data class Registration @JsonCreator constructor(
  @JsonProperty("type")
  val type: KeyValue,

  @JsonProperty("registerLevel")
  val registerLevel: KeyValue?,

  @JsonProperty("startDate")
  val startDate: LocalDate
)

private data class Registrations @JsonCreator constructor(
  @JsonProperty("registrations")
  val registrations: List<Registration>?
)
