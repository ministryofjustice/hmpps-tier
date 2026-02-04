package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import uk.gov.justice.digital.hmpps.hmppstier.client.SectionAnswer.*
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import java.io.Serializable
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
) : Serializable

@JsonIgnoreProperties(ignoreUnknown = true)
sealed interface NeedSection : Serializable {
    val section: Need
    val linkedToReOffending: YesNo
    val linkedToHarm: YesNo
    val questionAnswers: HashMap<String, SectionAnswer>

    @get:JsonIgnore
    val threshold: Threshold

    @get:JsonIgnore
    val sanThresholdOverride: Threshold?

    fun getScore(): Int? =
        if (questionAnswers.values.all { it == SectionAnswer.Problem.Missing || it == SectionAnswer.YesNo.Unknown }) {
            null
        } else {
            questionAnswers.values.sumOf { it.score }
        }

    fun getSeverity(sanIndicator: Boolean): NeedSeverity? {
        val threshold = if (sanIndicator) sanThresholdOverride ?: threshold else threshold
        return when {
            getScore() == null -> null
            getScore()!! >= threshold.severe -> NeedSeverity.SEVERE
            getScore()!! >= threshold.standard -> NeedSeverity.STANDARD
            else -> NeedSeverity.NO_NEED
        }
    }

    @JsonSerialize
    data class Accommodation(
        override val linkedToHarm: YesNo = YesNo.Unknown,
        override val linkedToReOffending: YesNo = YesNo.Unknown,
        override val questionAnswers: HashMap<String, SectionAnswer> = hashMapOf(),
    ) : NeedSection {
        override val section = Need.ACCOMMODATION
        override val threshold = Threshold(2, 7)
        override val sanThresholdOverride = Threshold(3, 5)

        @get:JsonIgnore
        val noFixedAbodeOrTransient = questionAnswers["noFixedAbodeOrTransient"] ?: YesNo.Unknown

        @get:JsonIgnore
        val suitabilityOfAccommodation = questionAnswers["suitabilityOfAccommodation"] ?: Problem.Missing

        @get:JsonIgnore
        val permanenceOfAccommodation = questionAnswers["permanenceOfAccommodation"] ?: Problem.Missing

        @get:JsonIgnore
        val locationOfAccommodation = questionAnswers["locationOfAccommodation"] ?: Problem.Missing
    }

    data class EducationTrainingEmployability(
        override val linkedToHarm: YesNo = YesNo.Unknown,
        override val linkedToReOffending: YesNo = YesNo.Unknown,
        override val questionAnswers: HashMap<String, SectionAnswer> = hashMapOf(),
    ) : NeedSection {
        override val section = Need.EDUCATION_TRAINING_AND_EMPLOYABILITY
        override val threshold = Threshold(3, 7)
        override val sanThresholdOverride = null

        @get:JsonIgnore
        val unemployed = questionAnswers["unemployed"] ?: Problem.Missing

        @get:JsonIgnore
        val employmentHistory = questionAnswers["employmentHistory"] ?: Problem.Missing

        @get:JsonIgnore
        val workRelatedSkills = questionAnswers["workRelatedSkills"] ?: Problem.Missing

        @get:JsonIgnore
        val attitudeToEmployment = questionAnswers["attitudeToEmployment"] ?: Problem.Missing
    }

    data class Relationships(
        override val linkedToHarm: YesNo = YesNo.Unknown,
        override val linkedToReOffending: YesNo = YesNo.Unknown,
        override val questionAnswers: HashMap<String, SectionAnswer> = hashMapOf(),
        val parentalResponsibilities: YesNo = YesNo.Unknown,
    ) : NeedSection {
        override val section = Need.RELATIONSHIPS
        override val threshold = Threshold(2, 5)
        override val sanThresholdOverride = null

        @get:JsonIgnore
        val relCloseFamily = questionAnswers["relCloseFamily"] ?: Problem.Missing

        @get:JsonIgnore
        val experienceOfChildhood = questionAnswers["experienceOfChildhood"] ?: Problem.Missing

        @get:JsonIgnore
        val prevCloseRelationships = questionAnswers["prevCloseRelationships"] ?: Problem.Missing
    }

    data class LifestyleAndAssociates(
        override val linkedToHarm: YesNo = YesNo.Unknown,
        override val linkedToReOffending: YesNo = YesNo.Unknown,
        override val questionAnswers: HashMap<String, SectionAnswer> = hashMapOf(),
    ) : NeedSection {
        override val section = Need.LIFESTYLE_AND_ASSOCIATES
        override val threshold = Threshold(2, 5)
        override val sanThresholdOverride = null

        @get:JsonIgnore
        val regActivitiesEncourageOffending = questionAnswers["regActivitiesEncourageOffending"] ?: Problem.Missing

        @get:JsonIgnore
        val easilyInfluenced = questionAnswers["easilyInfluenced"] ?: Problem.Missing

        @get:JsonIgnore
        val recklessness = questionAnswers["recklessness"] ?: Problem.Missing
    }

    data class DrugMisuse(
        override val linkedToHarm: YesNo = YesNo.Unknown,
        override val linkedToReOffending: YesNo = YesNo.Unknown,
        override val questionAnswers: HashMap<String, SectionAnswer> = hashMapOf(),
    ) : NeedSection {
        override val section = Need.DRUG_MISUSE
        override val threshold = Threshold(2, 8)
        override val sanThresholdOverride = Threshold(2, 6)

        @get:JsonIgnore
        val currentDrugNoted = questionAnswers["currentDrugNoted"] ?: Problem.Missing

        @get:JsonIgnore
        val everInjectedDrugs = questionAnswers["everInjectedDrugs"] ?: Frequency.Unknown

        @get:JsonIgnore
        val motivationToTackleDrugMisuse = questionAnswers["motivationToTackleDrugMisuse"] ?: Problem.Missing

        @get:JsonIgnore
        val drugsMajorActivity = questionAnswers["drugsMajorActivity"] ?: Problem.Missing
    }

    data class AlcoholMisuse(
        override val linkedToHarm: YesNo = YesNo.Unknown,
        override val linkedToReOffending: YesNo = YesNo.Unknown,
        override val questionAnswers: HashMap<String, SectionAnswer> = hashMapOf(),
    ) : NeedSection {
        override val section = Need.ALCOHOL_MISUSE
        override val threshold = Threshold(4, 7)
        override val sanThresholdOverride = Threshold(2, 4)

        @get:JsonIgnore
        val currentUse = questionAnswers["currentUse"] ?: Problem.Missing

        @get:JsonIgnore
        val bingeDrinking = questionAnswers["bingeDrinking"] ?: Problem.Missing

        @get:JsonIgnore
        val frequencyAndLevel = questionAnswers["frequencyAndLevel"] ?: Problem.Missing

        @get:JsonIgnore
        val alcoholTackleMotivation = questionAnswers["alcoholTackleMotivation"] ?: Problem.Missing
    }

    data class ThinkingAndBehaviour(
        override val linkedToHarm: YesNo = YesNo.Unknown,
        override val linkedToReOffending: YesNo = YesNo.Unknown,
        override val questionAnswers: HashMap<String, SectionAnswer> = hashMapOf(),
        val impulsivity: Problem = Problem.Missing,
        val temperControl: Problem = Problem.Missing,
    ) : NeedSection {
        override val section = Need.THINKING_AND_BEHAVIOUR
        override val threshold = Threshold(4, 7)
        override val sanThresholdOverride = Threshold(3, 5)

        @get:JsonIgnore
        val recogniseProblems = questionAnswers["recogniseProblems"] ?: Problem.Missing

        @get:JsonIgnore
        val problemSolvingSkills = questionAnswers["problemSolvingSkills"] ?: Problem.Missing

        @get:JsonIgnore
        val awarenessOfConsequences = questionAnswers["awarenessOfConsequences"] ?: Problem.Missing

        @get:JsonIgnore
        val understandsViewsOfOthers = questionAnswers["understandsViewsOfOthers"] ?: Problem.Missing
    }

    data class Attitudes(
        override val linkedToHarm: YesNo = YesNo.Unknown,
        override val linkedToReOffending: YesNo = YesNo.Unknown,
        override val questionAnswers: HashMap<String, SectionAnswer> = hashMapOf(),
    ) : NeedSection {
        override val section = Need.ATTITUDES
        override val threshold = Threshold(2, 7)
        override val sanThresholdOverride = Threshold(1, 5)

        @get:JsonIgnore
        val proCriminalAttitudes = questionAnswers["proCriminalAttitudes"] ?: Problem.Missing

        @get:JsonIgnore
        val attitudesTowardsSupervision = questionAnswers["attitudesTowardsSupervision"] ?: Problem.Missing

        @get:JsonIgnore
        val attitudesTowardsCommunitySociety = questionAnswers["attitudesTowardsCommunitySociety"] ?: Problem.Missing

        @get:JsonIgnore
        val motivationToAddressBehaviour = questionAnswers["motivationToAddressBehaviour"] ?: Problem.Missing
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
    val sanIndicator: Boolean,
) : Serializable

fun AssessmentSummary?.isSanAssessment() = this?.sanIndicator == true

data class Threshold(val standard: Int, val severe: Int) : Serializable