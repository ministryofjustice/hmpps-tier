package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.ArnsApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.AssessmentApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import java.time.Clock
import java.time.LocalDate.now

@Service
class AssessmentApiService(
    private val arnsApiClient: ArnsApiClient,
    private val assessmentApiClient: AssessmentApiClient,
    private val clock: Clock,
) {

    fun getAssessmentAnswers(assessmentId: String): Map<AdditionalFactorForWomen, String?> =
        assessmentApiClient.getAssessmentAnswers(assessmentId)
            .mapNotNull { question ->
                AdditionalFactorForWomen.from(question.questionCode)?.let {
                    it to question.answers.firstOrNull()?.refAnswerCode
                }
            }.toMap()

    fun getRecentAssessment(crn: String): OffenderAssessment? =
        arnsApiClient.getTimeline(crn).timeline
            .filter {
                it.status in COMPLETE_STATUSES && it.completedDate != null &&
                    it.completedDate.toLocalDate().isAfter(now(clock).minusWeeks(55).minusDays(1))
            }.maxByOrNull { it.completedDate!! }
            ?.let { OffenderAssessment(it.id.toString(), it.completedDate, null, it.status) }

    fun getAssessmentNeeds(crn: String): Map<Need, NeedSeverity> =
        arnsApiClient.getNeedsForCrn(crn).identifiedNeeds.associate { Need.valueOf(it.section) to it.severity }

    companion object {
        private val COMPLETE_STATUSES = listOf("COMPLETE", "LOCKED_INCOMPLETE")
    }
}
