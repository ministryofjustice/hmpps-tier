package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal
import java.time.LocalDate

@Component
class CommunityApiClient(@Qualifier("communityWebClientAppScope") private val webClient: WebClient) {

  @Cacheable(value = ["registration"], unless = "#result.size() == 0", key = "{ #crn }")
  fun getRegistrations(crn: String): Collection<Registration> {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/registrations")
      .retrieve()
      .bodyToMono(CommunityApiRegistrationsDto::class.java)
      .block()?.registrations ?: listOf()
  }

  @Cacheable(value = ["deliusAssessment"], key = "{ #crn }")
  fun getAssessments(crn: String): DeliusAssessmentsDto? {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/assessments")
      .retrieve()
      .bodyToMono(DeliusAssessmentsDto::class.java)
      .block()
  }

  fun getConvictions(crn: String): List<Conviction> {
    val responseType = object : ParameterizedTypeReference<List<Conviction>>() {}
    return webClient
      .get()
      .uri("/offenders/crn/$crn/convictions")
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
    return webClient
      .get()
      .uri("/offenders/crn/$crn/convictions/$convictionId/requirements")
      .retrieve()
      .bodyToMono(Requirements::class.java)
      .block()?.requirements ?: listOf()
  }
}

private data class Requirements @JsonCreator constructor(
  @JsonProperty("requirements")
  val requirements: List<Requirement>
)

data class Requirement @JsonCreator constructor(
  @JsonProperty("restrictive")
  val restrictive: Boolean?
)

private data class NsiWrapper @JsonCreator constructor(
  @JsonProperty("convictionId")
  val nsis: List<Nsi>,
)

data class Nsi @JsonCreator constructor(
  @JsonProperty("nsiStatus")
  val status: KeyValue
)

data class Conviction @JsonCreator constructor(
  @JsonProperty("convictionId")
  val convictionId: Long,

  @JsonProperty("sentence")
  val sentence: Sentence,
)

data class Sentence @JsonCreator constructor(
  @JsonProperty("terminationDate")
  var terminationDate: LocalDate?,

  @JsonProperty("sentenceType")
  val sentenceType: SentenceType,

  @JsonProperty("unpaidWork")
  val unpaidWork: UnpaidWork?
)

data class UnpaidWork @JsonCreator constructor(
  @JsonProperty("minutesOrdered")
  var minutesOrdered: String
)

data class SentenceType @JsonCreator constructor(
  @JsonProperty("code")
  var code: String
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
  val code: String,
  @JsonProperty("description")
  val description: String
)

data class Registration @JsonCreator constructor(

  @JsonProperty("type")
  val type: KeyValue,

  @JsonProperty("registerLevel")
  val registerLevel: KeyValue,

  @JsonProperty("active")
  val active: Boolean,

  @JsonProperty("startDate")
  val startDate: LocalDate
)

private data class CommunityApiRegistrationsDto @JsonCreator constructor(
  @JsonProperty("registrations")
  val registrations: List<Registration>?
)
