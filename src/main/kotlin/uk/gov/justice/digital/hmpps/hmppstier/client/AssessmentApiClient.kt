package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity

@Component
class AssessmentApiClient(@Qualifier("assessmentWebClientAppScope") private val webClient: WebClient) {

  fun getAssessmentAnswers(assessmentId: String, answerCodes: Collection<String>): Collection<Question> {
    return webClient
      .post()
      .uri("/assessments/oasysSetId/${assessmentId}/answers")
      .bodyValue(answerCodes)
      .retrieve()
      .bodyToMono(Answers::class.java)
      .block()?.questionAnswers ?: emptyList()
  }

  fun getAssessmentNeeds(assessmentId: String): Collection<AssessmentNeed> {
    val responseType = object : ParameterizedTypeReference<Collection<AssessmentNeed>>() {}
    return webClient
      .get()
      .uri("/assessments/oasysSetId/${assessmentId}/needs")
      .retrieve()
      .bodyToMono(responseType)
      .block() ?: emptyList()
  }

  @Cacheable(value = ["assessment"], key = "{ #crn }")
  fun getLatestAssessmentId(crn: String): String? {
    return webClient
      .get()
      .uri("/offenders/crn/${crn}/assessments/latest")
      .retrieve()
      .bodyToMono(Assessment::class.java)
      .block()?.assessmentId
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

private data class Answers @JsonCreator constructor(
  @JsonProperty("assessmentId")
  val questionAnswers: List<Question>,
)

private data class Assessment @JsonCreator constructor(
  @JsonProperty("assessmentId")
  val assessmentId: String?,
)
