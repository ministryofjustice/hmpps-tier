package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.AssessmentApiClient
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AssessmentComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import uk.gov.justice.digital.hmpps.hmppstier.service.exception.EntityNotFoundException

@Service
class AssessmentApiDataService(val assessmentApiClient: AssessmentApiClient) {

  fun getAssessmentComplexityAnswers(crn: String): Map<AssessmentComplexityFactor, String?> {
    assessmentApiClient.getLatestAssessmentId(crn)?.let { assessmentId ->
      return assessmentApiClient.getAssessmentAnswers(assessmentId)
        .filter { AssessmentComplexityFactor.from(it.questionCode) != null }
        .associateBy({ AssessmentComplexityFactor.from(it.questionCode)!! }, { it.answers.firstOrNull()?.refAnswerCode })
    }
    throw EntityNotFoundException("No latest Assessment found, can't get assessment answers")
  }

  fun getAssessmentNeeds(crn: String): Map<Need, NeedSeverity?> {
    assessmentApiClient.getLatestAssessmentId(crn)?.let { assessmentId ->
      return assessmentApiClient.getAssessmentNeeds(assessmentId)
        .filter { it.need != null }
        .associateBy({ it.need!! }, { it.severity })
    }
    throw EntityNotFoundException("No latest Assessment found, can't get assessment needs")
  }
}
