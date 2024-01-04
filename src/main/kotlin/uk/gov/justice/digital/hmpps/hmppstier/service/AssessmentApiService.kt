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
  private val clock: Clock,
) {

  fun getRecentAssessment(crn: String): OffenderAssessment? =
    assessmentApiClient.getAssessmentSummaries(crn)
      .filter {
        it.assessmentStatus in COMPLETE_STATUSES &&
          it.voided == null &&
          it.completed != null &&
          it.completed.toLocalDate().isAfter(LocalDate.now(clock).minusWeeks(55).minusDays(1))
      }
      .maxByOrNull { it.completed!! }
      .also {
        when (it) {
          null -> log.debug("No valid Assessment found for $crn")
          else -> log.debug("Found valid Assessment ${it.assessmentId} for $crn")
        }
      }

  fun getAssessmentAnswers(assessmentId: String): Map<AdditionalFactorForWomen, String?> =
    assessmentApiClient.getAssessmentAnswers(assessmentId)
      .mapNotNull { question ->
        AdditionalFactorForWomen.from(question.questionCode)?.let {
          it to question.answers.firstOrNull()?.refAnswerCode
        }
      }.toMap()

  fun getAssessmentNeeds(offenderAssessment: OffenderAssessment?): Map<Need, NeedSeverity> =
    offenderAssessment?.let { assessment ->
      assessmentApiClient.getAssessmentNeeds(assessment.assessmentId)
        .filter { it.need != null && it.severity != null }
        .associateBy({ it.need!! }, { it.severity!! })
    } ?: mapOf()

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val COMPLETE_STATUSES = listOf("COMPLETE", "LOCKED_INCOMPLETE")
  }
}
