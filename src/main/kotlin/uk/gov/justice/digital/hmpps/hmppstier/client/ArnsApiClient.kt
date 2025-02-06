package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import uk.gov.justice.digital.hmpps.hmppstier.client.SectionAnswer.Frequency
import uk.gov.justice.digital.hmpps.hmppstier.client.SectionAnswer.Problem
import uk.gov.justice.digital.hmpps.hmppstier.client.SectionAnswer.YesNo
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
        .uri("/tier-assessment/sections/{crn}", crn)
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
    val educationTrainingEmployability: NeedSection.EducationTrainingEmployability?,
    val relationships: NeedSection.Relationships?,
    val lifestyleAndAssociates: NeedSection.LifestyleAndAssociates?,
    val drugMisuse: NeedSection.DrugMisuse?,
    val alcoholMisuse: NeedSection.AlcoholMisuse?,
    val thinkingAndBehaviour: NeedSection.ThinkingAndBehaviour?,
    val attitudes: NeedSection.Attitudes?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
sealed interface NeedSection {
    val section: Need
    val linkedToReOffending: YesNo
    val linkedToHarm: YesNo
    val questionAnswers: Map<String, SectionAnswer>

    @get:JsonIgnore
    val threshold: Threshold

    fun getScore(): Int? =
        if (questionAnswers.values.all { it == SectionAnswer.Problem.Missing || it == SectionAnswer.YesNo.Unknown }) {
            null
        } else {
            questionAnswers.values.sumOf { it.score }
        }

    fun getSeverity(): NeedSeverity? = when {
        getScore() == null -> null
        getScore()!! >= threshold.severe -> NeedSeverity.SEVERE
        getScore()!! >= threshold.standard -> NeedSeverity.STANDARD
        else -> NeedSeverity.NO_NEED
    }

    data class Accommodation(
        override val linkedToHarm: YesNo = YesNo.Unknown,
        override val linkedToReOffending: YesNo = YesNo.Unknown,
        override val questionAnswers: Map<String, SectionAnswer> = mapOf(),
    ) : NeedSection {
        override val section = Need.ACCOMMODATION
        override val threshold = Threshold(2, 7)

        @get:JsonIgnore
        val noFixedAbodeOrTransient: YesNo by questionAnswers.withDefault { YesNo.Unknown }

        @get:JsonIgnore
        val suitabilityOfAccommodation: Problem by questionAnswers.withDefault { Problem.Missing }

        @get:JsonIgnore
        val permanenceOfAccommodation: Problem by questionAnswers.withDefault { Problem.Missing }

        @get:JsonIgnore
        val locationOfAccommodation: Problem by questionAnswers.withDefault { Problem.Missing }
    }

    data class EducationTrainingEmployability(
        override val linkedToHarm: YesNo = YesNo.Unknown,
        override val linkedToReOffending: YesNo = YesNo.Unknown,
        override val questionAnswers: Map<String, SectionAnswer> = mapOf(),
    ) : NeedSection {
        override val section = Need.EDUCATION_TRAINING_AND_EMPLOYABILITY
        override val threshold = Threshold(3, 7)

        @get:JsonIgnore
        val unemployed: Problem by questionAnswers.withDefault { Problem.Missing }

        @get:JsonIgnore
        val employmentHistory: Problem by questionAnswers.withDefault { Problem.Missing }

        @get:JsonIgnore
        val workRelatedSkills: Problem by questionAnswers.withDefault { Problem.Missing }

        @get:JsonIgnore
        val attitudeToEmployment: Problem by questionAnswers.withDefault { Problem.Missing }
    }

    data class Relationships(
        override val linkedToHarm: YesNo = YesNo.Unknown,
        override val linkedToReOffending: YesNo = YesNo.Unknown,
        override val questionAnswers: Map<String, SectionAnswer> = mapOf(),
        val parentalResponsibilities: YesNo = YesNo.Unknown,
    ) : NeedSection {
        override val section = Need.RELATIONSHIPS
        override val threshold = Threshold(2, 5)

        @get:JsonIgnore
        val relCloseFamily: Problem by questionAnswers.withDefault { Problem.Missing }

        @get:JsonIgnore
        val experienceOfChildhood: Problem by questionAnswers.withDefault { Problem.Missing }

        @get:JsonIgnore
        val prevCloseRelationships: Problem by questionAnswers.withDefault { Problem.Missing }
    }

    data class LifestyleAndAssociates(
        override val linkedToHarm: YesNo = YesNo.Unknown,
        override val linkedToReOffending: YesNo = YesNo.Unknown,
        override val questionAnswers: Map<String, SectionAnswer> = mapOf(),
    ) : NeedSection {
        override val section = Need.LIFESTYLE_AND_ASSOCIATES
        override val threshold = Threshold(2, 5)

        @get:JsonIgnore
        val regActivitiesEncourageOffending: Problem by questionAnswers.withDefault { Problem.Missing }

        @get:JsonIgnore
        val easilyInfluenced: Problem by questionAnswers.withDefault { Problem.Missing }

        @get:JsonIgnore
        val recklessness: Problem by questionAnswers.withDefault { Problem.Missing }
    }

    data class DrugMisuse(
        override val linkedToHarm: YesNo = YesNo.Unknown,
        override val linkedToReOffending: YesNo = YesNo.Unknown,
        override val questionAnswers: Map<String, SectionAnswer> = mapOf(),
    ) : NeedSection {
        override val section = Need.DRUG_MISUSE
        override val threshold = Threshold(2, 8)

        @get:JsonIgnore
        val currentDrugNoted: Problem by questionAnswers.withDefault { Problem.Missing }

        @get:JsonIgnore
        val everInjectedDrugs: Frequency by questionAnswers.withDefault { Frequency.Unknown }

        @get:JsonIgnore
        val motivationToTackleDrugMisuse: Problem by questionAnswers.withDefault { Problem.Missing }

        @get:JsonIgnore
        val drugsMajorActivity: Problem by questionAnswers.withDefault { Problem.Missing }
    }

    data class AlcoholMisuse(
        override val linkedToHarm: YesNo = YesNo.Unknown,
        override val linkedToReOffending: YesNo = YesNo.Unknown,
        override val questionAnswers: Map<String, SectionAnswer> = mapOf(),
    ) : NeedSection {
        override val section = Need.ALCOHOL_MISUSE
        override val threshold = Threshold(4, 7)

        @get:JsonIgnore
        val currentUse: Problem by questionAnswers.withDefault { Problem.Missing }

        @get:JsonIgnore
        val bingeDrinking: Problem by questionAnswers.withDefault { Problem.Missing }

        @get:JsonIgnore
        val frequencyAndLevel: Problem by questionAnswers.withDefault { Problem.Missing }

        @get:JsonIgnore
        val alcoholTackleMotivation: Problem by questionAnswers.withDefault { Problem.Missing }
    }

    data class ThinkingAndBehaviour(
        override val linkedToHarm: YesNo = YesNo.Unknown,
        override val linkedToReOffending: YesNo = YesNo.Unknown,
        override val questionAnswers: Map<String, SectionAnswer> = mapOf(),
        val impulsivity: Problem = Problem.Missing,
        val temperControl: Problem = Problem.Missing,
    ) : NeedSection {
        override val section = Need.THINKING_AND_BEHAVIOUR
        override val threshold = Threshold(4, 7)

        @get:JsonIgnore
        val recogniseProblems: Problem by questionAnswers.withDefault { Problem.Missing }

        @get:JsonIgnore
        val problemSolvingSkills: Problem by questionAnswers.withDefault { Problem.Missing }

        @get:JsonIgnore
        val awarenessOfConsequences: Problem by questionAnswers.withDefault { Problem.Missing }

        @get:JsonIgnore
        val understandsViewsOfOthers: Problem by questionAnswers.withDefault { Problem.Missing }
    }

    data class Attitudes(
        override val linkedToHarm: YesNo = YesNo.Unknown,
        override val linkedToReOffending: YesNo = YesNo.Unknown,
        override val questionAnswers: Map<String, SectionAnswer> = mapOf(),
    ) : NeedSection {
        override val section = Need.ATTITUDES
        override val threshold = Threshold(2, 7)

        @get:JsonIgnore
        val proCriminalAttitudes: Problem by questionAnswers.withDefault { Problem.Missing }

        @get:JsonIgnore
        val attitudesTowardsSupervision: Problem by questionAnswers.withDefault { Problem.Missing }

        @get:JsonIgnore
        val attitudesTowardsCommunitySociety: Problem by questionAnswers.withDefault { Problem.Missing }

        @get:JsonIgnore
        val motivationToAddressBehaviour: Problem by questionAnswers.withDefault { Problem.Missing }
    }
}

@JsonDeserialize(using = SectionAnswerDeserialiser::class)
sealed interface SectionAnswer {
    val score: Int

    enum class YesNo(override val score: Int) : SectionAnswer {
        Yes(2), No(0), Unknown(0)
    }

    enum class Problem(override val score: Int) : SectionAnswer {
        None(0), Some(1), Significant(2), Missing(0)
    }

    enum class Frequency(override val score: Int) : SectionAnswer {
        Never(0), Previous(1), Currently(2), Unknown(0)
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

data class Threshold(val standard: Int, val severe: Int)