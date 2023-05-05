package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.math.BigDecimal
import java.time.LocalDate

@Component
class CommunityApiClient(@Qualifier("communityWebClientAppScope") private val webClient: WebClient) {

  suspend fun getRegistrations(crn: String): Collection<Registration> =
    webClient
      .get()
      .uri("/offenders/crn/$crn/registrations?activeOnly=true")
      .retrieve()
      .awaitBody<Registrations>().registrations ?: listOf()

  suspend fun getDeliusAssessments(crn: String): DeliusAssessmentsDto? {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/assessments")
      .retrieve()
      .awaitBody()
  }
}

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
  val sentenceType: KeyValue,
)

data class DeliusAssessmentsDto @JsonCreator constructor(
  @JsonProperty("rsrScore")
  val rsr: BigDecimal?,
  @JsonProperty("ogrsScore")
  val ogrs: Int?,
)

data class KeyValue @JsonCreator constructor(
  @JsonProperty("code")
  val code: String,
)

data class Registration @JsonCreator constructor(
  @JsonProperty("type")
  val type: KeyValue,

  @JsonProperty("registerLevel")
  val registerLevel: KeyValue?,

  @JsonProperty("startDate")
  val startDate: LocalDate,
)

private data class Registrations @JsonCreator constructor(
  @JsonProperty("registrations")
  val registrations: List<Registration>?,
)
