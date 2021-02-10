package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AssessmentComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import java.time.LocalDate

@Component
class AssessmentApiClient(@Qualifier("assessmentWebClientAppScope") private val webClient: WebClient) {

  fun getAssessmentAnswers(assessmentId: String): Collection<Question> {
    return webClient
      .post()
      .uri("/assessments/oasysSetId/$assessmentId/answers")
      .bodyValue(AssessmentComplexityFactor.values().map { it.answerCode })
      .retrieve()
      .bodyToMono(Answers::class.java)
      .block()?.questionAnswers ?: emptyList()
  }

  fun getAssessmentNeeds(assessmentId: String): Collection<AssessmentNeed> {
    val responseType = object : ParameterizedTypeReference<Collection<AssessmentNeed>>() {}
    return webClient
      .get()
      .uri("/assessments/oasysSetId/$assessmentId/needs")
      .retrieve()
      .bodyToMono(responseType)
      .block() ?: emptyList()
  }

  @Cacheable(value = ["oasysAssessment"], key = "{ #crn }")
  fun getLatestAssessment(crn: String): Assessment? {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/assessments/latest")
      .retrieve()
      .bodyToMono(Assessment::class.java)
      .block()
  }
}

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

data class Assessment @JsonCreator constructor(
  @JsonProperty("assessmentId")
  val assessmentId: String,

  @JsonProperty("completed")
  val completed: LocalDate,
)

private data class Answers @JsonCreator constructor(
  @JsonProperty("questionAnswers")
  val questionAnswers: List<Question>,
)
