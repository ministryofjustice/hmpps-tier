package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.AssessmentApiClient
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AssessmentComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import java.time.Clock
import java.time.LocalDate

@Service
class AssessmentApiDataService(
  private val assessmentApiClient: AssessmentApiClient,
  private val clock: Clock
) {

  fun isAssessmentRecent(crn: String): Boolean {
    return assessmentApiClient.getLatestAssessment(crn).let {
      val cutOff = LocalDate.now(clock).minusWeeks(55).minusDays(1)
      it.completed.toLocalDate().isAfter(cutOff)
    }.also {
      log.debug("Assessment is recent for $crn: $it ")
    }
  }
  fun getAssessmentComplexityAnswers(crn: String): Map<AssessmentComplexityFactor, String?> {
    return assessmentApiClient.getLatestAssessment(crn).let { assessment ->
      assessmentApiClient.getAssessmentAnswers(assessment.assessmentId)
        .filter { AssessmentComplexityFactor.from(it.questionCode) != null }
        .associateBy({ AssessmentComplexityFactor.from(it.questionCode)!! }, { it.answers.firstOrNull()?.refAnswerCode })
    }.also {
      log.debug("Assessment Complexity answers for $crn: $it ")
    }
  }

  fun getAssessmentNeeds(crn: String): Map<Need, NeedSeverity?> {
    return assessmentApiClient.getLatestAssessment(crn).let { assessment ->
      assessmentApiClient.getAssessmentNeeds(assessment.assessmentId)
        .filter { it.need != null }
        .associateBy({ it.need!! }, { it.severity })
    }.also {
      log.debug("Assessment Needs for $crn: $it ")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(AssessmentApiDataService::class.java)
  }
}
