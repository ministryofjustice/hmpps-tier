package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.AssessmentApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AssessmentComplexityFactor
import java.time.Clock
import java.time.LocalDate

@Service
class AssessmentApiService(
  private val assessmentApiClient: AssessmentApiClient,
  private val clock: Clock
) {

  fun getRecentAssessment(crn: String): OffenderAssessment? =
    assessmentApiClient.getAssessmentSummaries(crn)
      .filter {
        it.voided == null &&
          it.completed != null &&
          it.completed.toLocalDate().isAfter(LocalDate.now(clock).minusWeeks(55).minusDays(1))
      }
      .maxByOrNull { it.completed!! }
      .also {
        when (it) {
          null -> log.warn("No valid Assessment found for $crn")
          else -> log.info("Found valid Assessment ${it.assessmentId} for $crn")
        }
      }

  fun getAssessmentAnswers(assessmentId: String): Map<AssessmentComplexityFactor?, String?> =
    assessmentApiClient.getAssessmentAnswers(assessmentId).associateBy(
      { AssessmentComplexityFactor.from(it.questionCode) },
      { it.answers.firstOrNull()?.refAnswerCode }
    )
      .filterKeys { it != null }

  companion object {
    private val log = LoggerFactory.getLogger(AssessmentApiService::class.java)
  }
}
