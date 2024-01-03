package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import java.time.LocalDateTime

@Component
class AssessmentApiClient(
    @Qualifier("assessmentWebClientAppScope") private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
) {

  fun getAssessmentAnswers(assessmentId: String): List<Question> {
    return getAssessmentAnswersCall(assessmentId)
  }

  fun getAssessmentNeeds(assessmentId: String): List<AssessmentNeed> {
    return restClient
      .get()
      .uri("/assessments/oasysSetId/$assessmentId/needs")
      .exchange { _, res ->
        when (res.statusCode) {
          OK -> objectMapper.readValue(res.body)
          NOT_FOUND -> listOf()
          else -> throw HttpClientErrorException(res.statusCode, res.statusText)
        }
      }
  }

  fun getAssessmentSummaries(crn: String): List<OffenderAssessment> {
    return restClient
      .get()
      .uri("/offenders/crn/$crn/assessments/summary?assessmentType=LAYER_3")
      .exchange { _, res ->
        when (res.statusCode) {
          OK -> objectMapper.readValue(res.body)
          NOT_FOUND -> listOf()
          else -> throw HttpClientErrorException(res.statusCode, res.statusText)
        }
      }
  }

  private fun getAssessmentAnswersCall(assessmentId: String): List<Question> {
    return restClient
      .post()
      .uri("/assessments/oasysSetId/$assessmentId/answers")
      .body(
        AdditionalFactorForWomen.entries.groupBy { it.section }
          .mapValues { it.value.map { q -> q.answerCode } },
      )
      .exchange { _, res ->
        when (res.statusCode) {
          OK -> objectMapper.readValue<Answers>(res.body).questionAnswers
          NOT_FOUND -> listOf()
          else -> throw HttpClientErrorException(res.statusCode, res.statusText)
        }
      }
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
  val assessmentStatus: String?,
)

data class AssessmentNeed @JsonCreator constructor(
  @JsonProperty("section")
  val need: Need?,

  @JsonProperty("severity")
  val severity: NeedSeverity?,
)

data class Question @JsonCreator constructor(
  @JsonProperty("refQuestionCode")
  val questionCode: String?,

  @JsonProperty("answers")
  val answers: Set<Answer>,
)

data class Answer @JsonCreator constructor(
  @JsonProperty("refAnswerCode")
  val refAnswerCode: String?,
)

private data class Answers @JsonCreator constructor(
  @JsonProperty("questionAnswers")
  val questionAnswers: List<Question>,
)
