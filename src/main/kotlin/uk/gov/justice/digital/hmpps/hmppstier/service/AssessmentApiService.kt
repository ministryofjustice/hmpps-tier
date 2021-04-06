package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.AssessmentApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
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
        it.assessmentStatus == "COMPLETE" &&
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

  fun getAssessmentAnswers(assessmentId: String): Map<AdditionalFactorForWomen?, String?> =
    assessmentApiClient.getAssessmentAnswers(assessmentId).associateBy(
      { AdditionalFactorForWomen.from(it.questionCode) },
      { it.answers.firstOrNull()?.refAnswerCode }
    ).filterKeys { it != null }

  fun getAssessmentNeeds(assessmentId: String): Map<Need, NeedSeverity?> =
    assessmentApiClient.getAssessmentNeeds(assessmentId)
      .filter { it.need != null }
      .associateBy({ it.need!! }, { it.severity })

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
