package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal
import java.time.LocalDate

@Component
class CommunityApiClient(@Qualifier("communityWebClientAppScope") private val webClient: WebClient) {

  fun getRegistrations(crn: String): Collection<Registration> =
    webClient
      .get()
      .uri("/offenders/crn/$crn/registrations?activeOnly=true")
      .retrieve()
      .bodyToMono(Registrations::class.java)
      .block()?.registrations ?: listOf()

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

  fun getOffender(crn: String): Offender? = webClient
    .get()
    .uri("/offenders/crn/$crn/all")
    .retrieve()
    .bodyToMono(Offender::class.java)
    .block()

  fun getRequirements(crn: String, convictionId: Long): List<RequirementDto> =
    webClient
      .get()
      .uri { uriBuilder ->
        uriBuilder.path("/offenders/crn/{crn}/convictions/{convictionId}/requirements")
          .queryParam("activeOnly", true)
          .queryParam("excludeSoftDeleted", true)
          .build(crn, convictionId)
      }
      .retrieve()
      .bodyToMono(Requirements::class.java)
      .block()?.requirements ?: listOf()
}

private data class Requirements @JsonCreator constructor(
  @JsonProperty("requirements")
  val requirements: List<RequirementDto>
)

data class RequirementDto @JsonCreator constructor(
  @JsonProperty("restrictive")
  val restrictive: Boolean?,

  @JsonProperty("requirementTypeMainCategory")
  val requirementTypeMainCategory: RequirementTypeMainCategory?
)

data class RequirementTypeMainCategory @JsonCreator constructor(
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
)

data class SentenceDto @JsonCreator constructor(
  @JsonProperty("terminationDate")
  val terminationDate: LocalDate?,

  @JsonProperty("sentenceType")
  val sentenceType: KeyValue
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
