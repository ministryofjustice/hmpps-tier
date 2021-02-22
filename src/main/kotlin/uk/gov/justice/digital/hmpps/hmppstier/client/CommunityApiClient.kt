package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppstier.service.exception.EntityNotFoundException
import java.math.BigDecimal
import java.time.LocalDate

@Component
class CommunityApiClient(@Qualifier("communityWebClientAppScope") private val webClient: WebClient) {

  @Cacheable(value = ["registration"], key = "{ #crn }")
  fun getRegistrations(crn: String): Collection<Registration> {
    return getRegistrationsCall(crn).also {
      log.info("Fetched ${it.size} Registrations for $crn")
      log.debug(it.toString())
    }
  }

  @Cacheable(value = ["deliusAssessment"], key = "{ #crn }")
  fun getAssessments(crn: String): DeliusAssessmentsDto? {
    return getAssessmentsCall(crn).also {
      log.info("Fetched Delius Assessment scores for $crn")
      log.debug(it.toString())
    }
  }

  @Cacheable(value = ["conviction"], key = "{ #crn }")
  fun getConvictions(crn: String): List<Conviction> {
    return getConvictionsCall(crn).also {
      log.info("Fetched ${it.size} Convictions for $crn")
      log.debug(it.toString())
    }
  }

  fun getBreachRecallNsis(crn: String, convictionId: Long): List<Nsi> {
    return getBreachRecallNsisCall(crn, convictionId).also {
      log.info("Fetched ${it.size} Convictions for $crn convictionId: $convictionId")
      log.debug(it.toString())
    }
  }

  fun getOffender(crn: String): Offender {
    return getOffenderCall(crn).also {
      log.info("Fetched Offender record for $crn")
      log.debug(it.toString())
    }
  }

  fun getRequirements(crn: String, convictionId: Long): List<Requirement> {
    return getRequirementsCall(crn, convictionId).also {
      log.info("Fetched Requirements for $crn convictionId: $convictionId")
      log.debug(it.toString())
    }
  }

  fun updateTier(tier: String, crn: String): ResponseEntity<Void>? {
    return updateTierCall(tier, crn).also {
      log.info("Updated Tier for $crn")
      log.debug("Body: $tier for $crn")
    }
  }

  private fun getRegistrationsCall(crn: String): Collection<Registration> {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/registrations")
      .retrieve()
      .bodyToMono(CommunityApiRegistrationsDto::class.java)
      .block()?.registrations ?: listOf()
  }

  private fun getAssessmentsCall(crn: String): DeliusAssessmentsDto? {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/assessments")
      .retrieve()
      .bodyToMono(DeliusAssessmentsDto::class.java)
      .block()
  }

  private fun getConvictionsCall(crn: String): List<Conviction> {
    val responseType = object : ParameterizedTypeReference<List<Conviction>>() {}
    return webClient
      .get()
      .uri("/offenders/crn/$crn/convictions")
      .retrieve()
      .bodyToMono(responseType)
      .block() ?: listOf()
  }

  private fun getBreachRecallNsisCall(crn: String, convictionId: Long): List<Nsi> {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/convictions/$convictionId/nsis?nsiCodes=BRE,BRES,REC,RECS")
      .retrieve()
      .bodyToMono(NsiWrapper::class.java)
      .block()?.nsis ?: listOf()
  }

  private fun getOffenderCall(crn: String): Offender {
    return webClient
      .get()
      .uri("/offenders/crn/$crn")
      .retrieve()
      .bodyToMono(Offender::class.java)
      .block() ?: throw EntityNotFoundException("No Offender record found for $crn")
  }

  private fun getRequirementsCall(crn: String, convictionId: Long): List<Requirement> {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/convictions/$convictionId/requirements")
      .retrieve()
      .bodyToMono(Requirements::class.java)
      .block()?.requirements ?: listOf()
  }

  private fun updateTierCall(tier: String, crn: String): ResponseEntity<Void>? {
    return webClient
      .post()
      .uri("/offenders/crn/$crn/tier/$tier")
      .retrieve().toBodilessEntity().block()
  }

  companion object {
    private val log = LoggerFactory.getLogger(AssessmentApiClient::class.java)
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
  val registerLevel: KeyValue?,

  @JsonProperty("active")
  val active: Boolean,

  @JsonProperty("startDate")
  val startDate: LocalDate
)

private data class CommunityApiRegistrationsDto @JsonCreator constructor(
  @JsonProperty("registrations")
  val registrations: List<Registration>?
)
