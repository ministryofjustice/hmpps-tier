package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import java.time.Clock
import java.time.LocalDate

@Service
class AssessmentApiService(private val clock: Clock) {

  fun isAssessmentRecent(crn: String, offenderAssessment: OffenderAssessment?): Boolean {
    return offenderAssessment?.let {
      val cutOff = LocalDate.now(clock).minusWeeks(55).minusDays(1)
      it.completed?.toLocalDate()?.isAfter(cutOff)
    }.also {
      log.debug("Assessment $it is recent for $crn")
    } ?: false
  }

  fun getLatestCompletedAssessment(crn: String, offenderAssessments: Collection<OffenderAssessment>): OffenderAssessment? =
    offenderAssessments
      .filter { it.voided == null && it.completed != null }
      .maxByOrNull { it.completed!! }
      ?.also { log.info("Found valid Assessment ${it.assessmentId} for $crn") }

  companion object {
    private val log = LoggerFactory.getLogger(AssessmentApiService::class.java)
  }
}
