package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import java.time.LocalDateTime

@Component
class ArnsApiClient(
    @Qualifier("arnsRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper
) {
    fun getTierAssessmentInformation(crn: String): AssessmentForTier? = restClient
        .get()
        .uri("/tier-assessment/sections/$crn")
        .exchange<AssessmentForTier?> { _, res ->
            when (res.statusCode) {
                HttpStatus.OK -> objectMapper.readValue(res.body)
                HttpStatus.NOT_FOUND -> null
                else -> throw HttpClientErrorException(res.statusCode, res.statusText)
            }
        }
}

data class AssessmentForTier(
    val assessment: AssessmentSummary?,
    val accommodation: NeedSection.Accommodation?,
    @JsonAlias("educationTrainingEmployment")
    val educationTrainingEmployability: NeedSection.EducationTrainingEmployability?,
    val relationships: NeedSection.Relationships?,
    val lifestyleAndAssociates: NeedSection.LifestyleAndAssociates?,
    val drugMisuse: NeedSection.DrugMisuse?,
    val alcoholMisuse: NeedSection.AlcoholMisuse?,
    val thinkingAndBehaviour: NeedSection.ThinkingAndBehaviour?,
    val attitudes: NeedSection.Attitudes?,
)

sealed interface NeedSection {
    val section: Need
    val severity: NeedSeverity?

    data class Accommodation(override val severity: NeedSeverity?) : NeedSection {
        override val section = Need.ACCOMMODATION
    }

    data class EducationTrainingEmployability(override val severity: NeedSeverity?) : NeedSection {
        override val section = Need.EDUCATION_TRAINING_AND_EMPLOYABILITY
    }

    data class Relationships(
        override val severity: NeedSeverity?,
        val parentalResponsibilities: SectionAnswer.YesNo
    ) : NeedSection {
        override val section = Need.RELATIONSHIPS
    }

    data class LifestyleAndAssociates(override val severity: NeedSeverity?) : NeedSection {
        override val section = Need.LIFESTYLE_AND_ASSOCIATES
    }

    data class DrugMisuse(override val severity: NeedSeverity?) : NeedSection {
        override val section = Need.DRUG_MISUSE
    }

    data class AlcoholMisuse(override val severity: NeedSeverity?) : NeedSection {
        override val section = Need.ALCOHOL_MISUSE
    }

    data class ThinkingAndBehaviour(
        override val severity: NeedSeverity?,
        val impulsivity: SectionAnswer.Problem,
        val temperControl: SectionAnswer.Problem
    ) : NeedSection {
        override val section = Need.THINKING_AND_BEHAVIOUR
    }

    data class Attitudes(override val severity: NeedSeverity?) : NeedSection {
        override val section = Need.ATTITUDES
    }
}

sealed interface SectionAnswer {
    val score: Int

    enum class YesNo(override val score: Int) : SectionAnswer {
        Yes(2), No(0), Unknown(0)
    }

    enum class Problem(override val score: Int) : SectionAnswer {
        None(0), Some(1), Significant(2), Missing(0)
    }
}

data class AssessmentSummary(
    @JsonAlias("assessmentId")
    val id: Long,
    val completedDate: LocalDateTime?,
    @JsonAlias("assessmentType")
    val type: String,
    val status: String,
)