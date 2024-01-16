package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import java.time.LocalDateTime

@Component
class ArnsApiClient(
    @Qualifier("arnsRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper
) {
    fun getTimeline(crn: String): Timeline = restClient
        .get()
        .uri("/assessments/timeline/crn/$crn")
        .exchange { req, res ->
            when (res.statusCode) {
                HttpStatus.OK -> objectMapper.readValue(res.body)
                HttpStatus.NOT_FOUND -> Timeline()
                else -> throw HttpClientErrorException(res.statusCode, res.statusText)
            }
        }

    fun getNeedsForCrn(crn: String): AssessedNeeds = restClient
        .get()
        .uri("/needs/crn/$crn")
        .exchange { _, res ->
            when (res.statusCode) {
                HttpStatus.OK -> objectMapper.readValue(res.body)
                HttpStatus.NOT_FOUND -> AssessedNeeds()
                else -> throw HttpClientErrorException(res.statusCode, res.statusText)
            }
        }
}

data class AssessedNeed(
    val section: String,
    val name: String,
    val overThreshold: Boolean,
    val riskOfHarm: Boolean,
    val riskOfReoffending: Boolean,
    val flaggedAsNeed: Boolean,
    val severity: NeedSeverity,
    val identifiedAsNeed: Boolean,
    val needScore: Long,
)

data class AssessedNeeds(
    val identifiedNeeds: List<AssessedNeed> = listOf(),
    val assessedOn: LocalDateTime? = null
)

data class Timeline(val timeline: List<AssessmentSummary> = listOf())

data class AssessmentSummary(
    @JsonAlias("assessmentId")
    val id: Long,
    val completedDate: LocalDateTime?,
    @JsonAlias("assessmentType")
    val type: String,
    val status: String,
)