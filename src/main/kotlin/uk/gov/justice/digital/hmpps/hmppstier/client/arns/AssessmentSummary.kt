package uk.gov.justice.digital.hmpps.hmppstier.client.arns

import com.fasterxml.jackson.annotation.JsonAlias
import java.io.Serializable
import java.time.LocalDateTime

data class AssessmentSummary(
    @JsonAlias("assessmentId")
    val id: Long,
    val completedDate: LocalDateTime?,
    @JsonAlias("assessmentType")
    val type: String,
    val status: String,
    val sanIndicator: Boolean,
): Serializable