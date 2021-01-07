package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.AssessmentApiClient
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AssessmentComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity

@Service
class AssessmentApiDataService(val assessmentApiClient: AssessmentApiClient) {

  fun getAssessmentComplexityAnswers(crn: String): List<AssessmentComplexityFactor> {
    assessmentApiClient.getLatestAssessmentId(crn)?.let { assessmentId ->
      val allAnswers = assessmentApiClient.getAssessmentAnswers(
        assessmentId,
        AssessmentComplexityFactor.values().map { it.answerCode })
      return allAnswers.filter { question -> question.answers.any { isYes(it.refAnswerCode) } }
        .mapNotNull { it.questionCode?.let { code -> AssessmentComplexityFactor.from(code) } }
    }
    return emptyList()
  }

  fun getAssessmentNeeds(crn: String): Map<Need, NeedSeverity?> {
    assessmentApiClient.getLatestAssessmentId(crn)?.let { assessmentId ->
      val allNeeds = assessmentApiClient.getAssessmentNeeds(assessmentId)
      return allNeeds.filter { it.need != null }.associateBy({ it.need!! }, { it.severity })
    }
    return emptyMap()
  }

  private fun isYes(value: String?): Boolean {
    return "YES".equals(value, true) || "Y".equals(value, true)
  }

}
