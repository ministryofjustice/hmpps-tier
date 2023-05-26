package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchangeOrNull
import org.springframework.web.reactive.function.client.createExceptionAndAwait
import uk.gov.justice.digital.hmpps.hmppstier.config.Generated
import java.math.BigDecimal
import java.time.LocalDate

@Component
@Generated
class CommunityApiClient(@Qualifier("communityWebClientAppScope") private val webClient: WebClient) {

  suspend fun getRegistrations(crn: String): Collection<Registration>? =
    webClient
      .get()
      .uri("/offenders/crn/$crn/registrations?activeOnly=true")
      .awaitExchangeOrNull { response ->
        when (response.statusCode()) {
          HttpStatus.OK -> response.awaitBody<Registrations>().registrations ?: listOf()
          HttpStatus.NOT_FOUND -> emptyList()
          else -> throw response.createExceptionAndAwait()
        }
      }

  suspend fun getDeliusAssessments(crn: String): DeliusAssessmentsDto? {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/assessments")
      .awaitExchangeOrNull { response ->
        when (response.statusCode()) {
          HttpStatus.OK -> response.awaitBody<DeliusAssessmentsDto>()
          HttpStatus.NOT_FOUND -> DeliusAssessmentsDto(BigDecimal.valueOf(-2), -2)
          else -> throw response.createExceptionAndAwait()
        }
      }
  }

  suspend fun getConvictions(crn: String): List<ConvictionDto>? {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/convictions?activeOnly=true")
      .awaitExchangeOrNull { response ->
        when (response.statusCode()) {
          HttpStatus.OK -> response.awaitBody<List<ConvictionDto>>()
          HttpStatus.NOT_FOUND -> listOf(ConvictionDto(-2L, null))
          else -> throw response.createExceptionAndAwait()
        }
      }
  }

  suspend fun getBreachRecallNsis(crn: String, convictionId: Long): List<Nsi> {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/convictions/$convictionId/nsis?nsiCodes=BRE,BRES,REC,RECS")
      .retrieve()
      .awaitBody<NsiWrapper>().nsis
  }

  suspend fun getRequirements(crn: String, convictionId: Long): List<RequirementDto>? =
    webClient
      .get()
      .uri { uriBuilder ->
        uriBuilder.path("/offenders/crn/{crn}/convictions/{convictionId}/requirements")
          .queryParam("activeOnly", true)
          .queryParam("excludeSoftDeleted", true)
          .build(crn, convictionId)
      }
      .awaitExchangeOrNull { response ->
        when (response.statusCode()) {
          HttpStatus.OK -> response.awaitBody<Requirements>().requirements
          HttpStatus.NOT_FOUND -> emptyList()
          else -> throw response.createExceptionAndAwait()
        }
      }

  suspend fun getOffender(crn: String): Offender? {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/all")
      .awaitExchangeOrNull { response ->
        when (response.statusCode()) {
          HttpStatus.OK -> response.awaitBody<Offender>()
          HttpStatus.NOT_FOUND -> Offender("NOT_FOUND", "NOT_FOUND")
          else -> throw response.createExceptionAndAwait()
        }
      }
  }
}

@Generated
private data class Requirements @JsonCreator constructor(
  @JsonProperty("requirements")
  val requirements: List<RequirementDto>,
)

@Generated
data class RequirementDto @JsonCreator constructor(
  @JsonProperty("restrictive")
  val restrictive: Boolean?,

  @JsonProperty("requirementTypeMainCategory")
  val requirementTypeMainCategory: RequirementTypeMainCategory?,
)

@Generated
data class RequirementTypeMainCategory @JsonCreator constructor(
  @JsonProperty("code")
  val code: String,
)

@Generated
private data class NsiWrapper @JsonCreator constructor(
  @JsonProperty("nsis")
  val nsis: List<Nsi>,
)

@Generated
data class Nsi @JsonCreator constructor(
  @JsonProperty("nsiOutcome")
  val status: KeyValue?,
)

@Generated
data class ConvictionDto @JsonCreator constructor(
  @JsonProperty("convictionId")
  val convictionId: Long,

  @JsonProperty("sentence")
  val sentence: SentenceDto?,
)

@Generated
data class SentenceDto @JsonCreator constructor(
  @JsonProperty("terminationDate")
  val terminationDate: LocalDate?,

  @JsonProperty("sentenceType")
  val sentenceType: KeyValue,
)

@Generated
data class DeliusAssessmentsDto @JsonCreator constructor(
  @JsonProperty("rsrScore")
  val rsr: BigDecimal?,
  @JsonProperty("ogrsScore")
  val ogrs: Int?,
)

@Generated
data class KeyValue @JsonCreator constructor(
  @JsonProperty("code")
  val code: String,
)

@Generated
data class Registration @JsonCreator constructor(
  @JsonProperty("type")
  val type: KeyValue,

  @JsonProperty("registerLevel")
  val registerLevel: KeyValue?,

  @JsonProperty("startDate")
  val startDate: LocalDate,
)

@Generated
private data class Registrations @JsonCreator constructor(
  @JsonProperty("registrations")
  val registrations: List<Registration>?,
)

@Generated
data class Offender @JsonCreator constructor(
  @JsonProperty("gender")
  val gender: String,
  @JsonProperty("currentTier")
  val tier: String?,
)
