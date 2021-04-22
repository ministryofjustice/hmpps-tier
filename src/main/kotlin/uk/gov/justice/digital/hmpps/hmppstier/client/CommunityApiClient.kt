package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal
import java.time.LocalDate

@Component
class CommunityApiClient(@Qualifier("communityWebClientAppScope") private val webClient: WebClient) {

  fun getRegistrations(crn: String): Collection<Registration> {
    return getRegistrationsCall(crn).sortedByDescending { it.startDate }
      .also {
        log.info("Fetched ${it.size} Registrations for $crn")
      }
  }

  fun getDeliusAssessments(crn: String): DeliusAssessments? {
    return getAssessmentsCall(crn)
      .also {
        log.info("Fetched Delius Assessment scores for $crn")
      }
  }

  fun getConvictionsWithSentences(crn: String): List<Conviction> {
    val convictions = getConvictionsCall(crn)
    val sentences = convictions.mapNotNull { it.sentence }
    return convictions
      .filter { it.sentence in sentences }
      .map { Conviction(it.convictionId, it.sentence!!, it.offences.filterNotNull()) }
      .also {
        log.info("Fetched ${it.size} Convictions for $crn")
      }
  }

  fun getBreachRecallNsis(crn: String, convictionId: Long): List<Nsi> {
    return getBreachRecallNsisCall(crn, convictionId)
      .also {
        log.info("Fetched ${it.size} breach/recall NSIs for $crn convictionId: $convictionId")
      }
  }

  fun getOffender(crn: String): Offender? {
    return getOffenderCall(crn)
      .also {
        log.info("Fetched Offender record for $crn")
      }
  }

  fun getRequirements(crn: String, convictionId: Long): List<Requirement> {
    return getRequirementsCall(crn, convictionId)
      .filterNot { it.requirementTypeMainCategory == null && it.restrictive == null }
      .map { Requirement(it.restrictive!!, it.requirementTypeMainCategory!!.code) }
      .also {
        log.info("Fetched Requirements for $crn convictionId: $convictionId")
      }
  }

  private fun getRegistrationsCall(crn: String): Collection<Registration> {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/registrations?activeOnly=true")
      .retrieve()
      .bodyToMono(Registrations::class.java)
      .block()?.registrations ?: listOf()
  }

  private fun getAssessmentsCall(crn: String): DeliusAssessments? {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/assessments")
      .retrieve()
      .bodyToMono(DeliusAssessments::class.java)
      .block()
  }

  private fun getConvictionsCall(crn: String): List<ConvictionDto> {
    val responseType = object : ParameterizedTypeReference<List<ConvictionDto>>() {}
    return webClient
      .get()
      .uri("/offenders/crn/$crn/convictions?activeOnly=true")
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

  private fun getOffenderCall(crn: String): Offender? {
    return webClient
      .get()
      .uri("/offenders/crn/$crn")
      .retrieve()
      .bodyToMono(Offender::class.java)
      .block()
  }

  private fun getRequirementsCall(crn: String, convictionId: Long): List<RequirementDto> {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/convictions/$convictionId/requirements?activeOnly=true")
      .retrieve()
      .bodyToMono(Requirements::class.java)
      .block()?.requirements ?: listOf()
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
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

private data class ConvictionDto @JsonCreator constructor(
  @JsonProperty("convictionId")
  val convictionId: Long,

  @JsonProperty("sentence")
  val sentence: Sentence?,

  @JsonProperty("offences")
  val offences: List<Offence?>
)

data class Conviction constructor(
  val convictionId: Long,
  val sentence: Sentence,
  val offences: List<Offence>
)

data class Sentence @JsonCreator constructor(
  @JsonProperty("terminationDate")
  val terminationDate: LocalDate?,

  @JsonProperty("sentenceType")
  val sentenceType: KeyValue,

  @JsonProperty("startDate")
  val startDate: LocalDate,

  @JsonProperty("expectedSentenceEndDate")
  val expectedSentenceEndDate: LocalDate,

  @JsonProperty("latestCourtAppearanceOutcome")
  val latestCourtAppearanceOutcome: KeyValue
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

data class DeliusAssessments @JsonCreator constructor(
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

  @JsonProperty("startDate")
  val startDate: LocalDate
)

private data class Registrations @JsonCreator constructor(
  @JsonProperty("registrations")
  val registrations: List<Registration>?
)
