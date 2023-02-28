package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import org.springframework.web.reactive.function.client.bodyToFlow
import org.springframework.web.reactive.function.client.createExceptionAndAwait
import org.springframework.web.reactive.function.client.exchangeToFlow
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import java.time.LocalDateTime

@Component
class AssessmentApiClient(@Qualifier("assessmentWebClientAppScope") private val webClient: WebClient) {

  suspend fun getAssessmentAnswers(assessmentId: String): Collection<Question> {
    return getAssessmentAnswersCall(assessmentId)
  }

  suspend fun getAssessmentNeeds(assessmentId: String): Collection<AssessmentNeed> {
    return webClient
      .get()
      .uri("/assessments/oasysSetId/$assessmentId/needs")
      .retrieve()
      .bodyToFlow<AssessmentNeed>()
      .toList()
  }

  suspend fun getAssessmentSummaries(crn: String): Collection<OffenderAssessment> {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/assessments/summary?assessmentType=LAYER_3")
      .exchangeToFlow<OffenderAssessment> { response ->
        flow {
          when (response.statusCode()) {
            OK -> emitAll(response.bodyToFlow())
            NOT_FOUND -> emptyFlow<OffenderAssessment>()
            else -> throw response.createExceptionAndAwait()
          }
        }
      }.toList()
  }

  private suspend fun getAssessmentAnswersCall(assessmentId: String): Collection<Question> {
    return webClient
      .post()
      .uri("/assessments/oasysSetId/$assessmentId/answers")
      .bodyValue(
        AdditionalFactorForWomen.values().groupBy { it.section }
          .mapValues { it.value.map { q -> q.answerCode } }
      )
      .retrieve()
      .awaitBodyOrNull<Answers>()?.questionAnswers ?: emptyList()
  }
}

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
