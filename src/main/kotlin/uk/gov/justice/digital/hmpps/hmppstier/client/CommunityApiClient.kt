package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
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
}

data class DeliusAssessmentsDto @JsonCreator constructor(
  @JsonProperty("rsrScore")
  val rsr: BigDecimal?,
  @JsonProperty("OGRSScore")
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
  val registrations: List<Registration>
)
