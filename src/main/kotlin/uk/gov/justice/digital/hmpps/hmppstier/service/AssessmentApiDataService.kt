package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.AssessmentApiClient
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AssessmentComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import uk.gov.justice.digital.hmpps.hmppstier.service.exception.EntityNotFoundException
import java.time.Clock
import java.time.LocalDate

@Service
class AssessmentApiDataService(  private val assessmentApiClient: AssessmentApiClient,
                                 private val clock: Clock
) {

  fun isAssessmentRecent(crn: String): Boolean {
    assessmentApiClient.getLatestAssessment(crn)?.let {
      val cutOff = LocalDate.now(clock).minusWeeks(55).minusDays(1)
      return it.completed.toLocalDate().isAfter(cutOff)
    }
    return false
  }
  fun getAssessmentComplexityAnswers(crn: String): Map<AssessmentComplexityFactor, String?> {
    assessmentApiClient.getLatestAssessment(crn)?.let { assessment ->
      return assessmentApiClient.getAssessmentAnswers(assessment.assessmentId)
        .filter { AssessmentComplexityFactor.from(it.questionCode) != null }
        .associateBy({ AssessmentComplexityFactor.from(it.questionCode)!! }, { it.answers.firstOrNull()?.refAnswerCode })
    }
    throw EntityNotFoundException("No latest Assessment found, can't get assessment answers")
  }

  fun getAssessmentNeeds(crn: String): Map<Need, NeedSeverity?> {
    assessmentApiClient.getLatestAssessment(crn)?.let { assessment ->
      return assessmentApiClient.getAssessmentNeeds(assessment.assessmentId)
        .filter { it.need != null }
        .associateBy({ it.need!! }, { it.severity })
    }
    throw EntityNotFoundException("No latest Assessment found, can't get assessment needs")
  }
}
