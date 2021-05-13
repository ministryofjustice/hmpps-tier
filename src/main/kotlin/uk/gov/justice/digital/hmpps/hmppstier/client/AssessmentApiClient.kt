package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import java.time.LocalDateTime

@Component
class AssessmentApiClient(@Qualifier("assessmentWebClientAppScope") private val webClient: WebClient) {

  fun getAssessmentAnswers(assessmentId: String?): Collection<Question> {
    return assessmentId?.let {
      getAssessmentAnswersCall(assessmentId)
    } ?: listOf()
  }

  fun getAssessmentNeeds(assessmentId: String): Collection<AssessmentNeed> {
    return getAssessmentNeedsCall(assessmentId)
  }

  fun getAssessmentSummaries(crn: String): Collection<OffenderAssessment> {
    val responseType = object : ParameterizedTypeReference<Collection<OffenderAssessment>>() {}
    return webClient
      .get()
      .uri("/offenders/crn/$crn/assessments/summary?assessmentType=LAYER_3")
      .retrieve()
      .onStatus(
        { httpStatus -> NOT_FOUND == httpStatus },
        { Mono.error(MissingAssessmentError("No assessment found for $crn")) }
      )
      .bodyToMono(responseType)
      .onErrorResume { ex ->
        when (ex) {
          is MissingAssessmentError -> Mono.empty()
          else -> Mono.error(ex)
        }
      }
      .block() ?: emptyList()
  }

  private fun getAssessmentAnswersCall(assessmentId: String): Collection<Question> {
    return webClient
      .post()
      .uri("/assessments/oasysSetId/$assessmentId/answers")
      .bodyValue(
        AdditionalFactorForWomen.values().groupBy { it.section }
          .mapValues { it.value.map { q -> q.answerCode } }
      )
      .retrieve()
      .bodyToMono(Answers::class.java)
      .block()?.questionAnswers ?: emptyList()
  }

  private fun getAssessmentNeedsCall(assessmentId: String): Collection<AssessmentNeed> {
    val responseType = object : ParameterizedTypeReference<Collection<AssessmentNeed>>() {}
    return webClient
      .get()
      .uri("/assessments/oasysSetId/$assessmentId/needs")
      .retrieve()
      .bodyToMono(responseType)
      .block() ?: emptyList()
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

private class MissingAssessmentError(msg: String) : RuntimeException(msg)

data class OffenderAssessment @JsonCreator constructor(
  @JsonProperty("assessmentId")
  val assessmentId: String,

  @JsonProperty("completed")
  val completed: LocalDateTime?,

  @JsonProperty("voided")
  val voided: LocalDateTime?,

  @JsonProperty("assessmentStatus")
  val assessmentStatus: String?
)

data class AssessmentNeed @JsonCreator constructor(
  @JsonProperty("section")
  val need: Need?,

  @JsonProperty("severity")
  val severity: NeedSeverity?
)

data class Question @JsonCreator constructor(
  @JsonProperty("refQuestionCode")
  val questionCode: String?,

  @JsonProperty("answers")
  val answers: Set<Answer>
)

data class Answer @JsonCreator constructor(
  @JsonProperty("refAnswerCode")
  val refAnswerCode: String?
)

private data class Answers @JsonCreator constructor(
  @JsonProperty("questionAnswers")
  val questionAnswers: List<Question>,
)
