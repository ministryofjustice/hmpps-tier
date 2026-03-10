package uk.gov.justice.digital.hmpps.hmppstier.client.arns

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import java.io.Serializable

@JsonIgnoreProperties(ignoreUnknown = true)
sealed interface NeedSection : Serializable {
    val section: Need
    val linkedToReOffending: SectionAnswer.YesNo
    val linkedToHarm: SectionAnswer.YesNo
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
        override val linkedToHarm: SectionAnswer.YesNo = SectionAnswer.YesNo.Unknown,
        override val linkedToReOffending: SectionAnswer.YesNo = SectionAnswer.YesNo.Unknown,
        override val questionAnswers: HashMap<String, SectionAnswer> = hashMapOf(),
    ) : NeedSection {
        override val section = Need.ACCOMMODATION
        override val threshold = Threshold(2, 7)
        override val sanThresholdOverride = Threshold(3, 5)

        @get:JsonIgnore
        val noFixedAbodeOrTransient = questionAnswers["noFixedAbodeOrTransient"] ?: SectionAnswer.YesNo.Unknown

        @get:JsonIgnore
        val suitabilityOfAccommodation = questionAnswers["suitabilityOfAccommodation"] ?: SectionAnswer.Problem.Missing

        @get:JsonIgnore
        val permanenceOfAccommodation = questionAnswers["permanenceOfAccommodation"] ?: SectionAnswer.Problem.Missing

        @get:JsonIgnore
        val locationOfAccommodation = questionAnswers["locationOfAccommodation"] ?: SectionAnswer.Problem.Missing
    }

    data class EducationTrainingEmployability(
        override val linkedToHarm: SectionAnswer.YesNo = SectionAnswer.YesNo.Unknown,
        override val linkedToReOffending: SectionAnswer.YesNo = SectionAnswer.YesNo.Unknown,
        override val questionAnswers: HashMap<String, SectionAnswer> = hashMapOf(),
    ) : NeedSection {
        override val section = Need.EDUCATION_TRAINING_AND_EMPLOYABILITY
        override val threshold = Threshold(3, 7)
        override val sanThresholdOverride = null

        @get:JsonIgnore
        val unemployed = questionAnswers["unemployed"] ?: SectionAnswer.Problem.Missing

        @get:JsonIgnore
        val employmentHistory = questionAnswers["employmentHistory"] ?: SectionAnswer.Problem.Missing

        @get:JsonIgnore
        val workRelatedSkills = questionAnswers["workRelatedSkills"] ?: SectionAnswer.Problem.Missing

        @get:JsonIgnore
        val attitudeToEmployment = questionAnswers["attitudeToEmployment"] ?: SectionAnswer.Problem.Missing
    }

    data class Relationships(
        override val linkedToHarm: SectionAnswer.YesNo = SectionAnswer.YesNo.Unknown,
        override val linkedToReOffending: SectionAnswer.YesNo = SectionAnswer.YesNo.Unknown,
        override val questionAnswers: HashMap<String, SectionAnswer> = hashMapOf(),
        val parentalResponsibilities: SectionAnswer.YesNo = SectionAnswer.YesNo.Unknown,
    ) : NeedSection {
        override val section = Need.RELATIONSHIPS
        override val threshold = Threshold(2, 5)
        override val sanThresholdOverride = null

        @get:JsonIgnore
        val relCloseFamily = questionAnswers["relCloseFamily"] ?: SectionAnswer.Problem.Missing

        @get:JsonIgnore
        val experienceOfChildhood = questionAnswers["experienceOfChildhood"] ?: SectionAnswer.Problem.Missing

        @get:JsonIgnore
        val prevCloseRelationships = questionAnswers["prevCloseRelationships"] ?: SectionAnswer.Problem.Missing
    }

    data class LifestyleAndAssociates(
        override val linkedToHarm: SectionAnswer.YesNo = SectionAnswer.YesNo.Unknown,
        override val linkedToReOffending: SectionAnswer.YesNo = SectionAnswer.YesNo.Unknown,
        override val questionAnswers: HashMap<String, SectionAnswer> = hashMapOf(),
    ) : NeedSection {
        override val section = Need.LIFESTYLE_AND_ASSOCIATES
        override val threshold = Threshold(2, 5)
        override val sanThresholdOverride = null

        @get:JsonIgnore
        val regActivitiesEncourageOffending =
            questionAnswers["regActivitiesEncourageOffending"] ?: SectionAnswer.Problem.Missing

        @get:JsonIgnore
        val easilyInfluenced = questionAnswers["easilyInfluenced"] ?: SectionAnswer.Problem.Missing

        @get:JsonIgnore
        val recklessness = questionAnswers["recklessness"] ?: SectionAnswer.Problem.Missing
    }

    data class DrugMisuse(
        override val linkedToHarm: SectionAnswer.YesNo = SectionAnswer.YesNo.Unknown,
        override val linkedToReOffending: SectionAnswer.YesNo = SectionAnswer.YesNo.Unknown,
        override val questionAnswers: HashMap<String, SectionAnswer> = hashMapOf(),
    ) : NeedSection {
        override val section = Need.DRUG_MISUSE
        override val threshold = Threshold(2, 8)
        override val sanThresholdOverride = Threshold(2, 6)

        @get:JsonIgnore
        val currentDrugNoted = questionAnswers["currentDrugNoted"] ?: SectionAnswer.Problem.Missing

        @get:JsonIgnore
        val everInjectedDrugs = questionAnswers["everInjectedDrugs"] ?: SectionAnswer.Frequency.Unknown

        @get:JsonIgnore
        val motivationToTackleDrugMisuse =
            questionAnswers["motivationToTackleDrugMisuse"] ?: SectionAnswer.Problem.Missing

        @get:JsonIgnore
        val drugsMajorActivity = questionAnswers["drugsMajorActivity"] ?: SectionAnswer.Problem.Missing
    }

    data class AlcoholMisuse(
        override val linkedToHarm: SectionAnswer.YesNo = SectionAnswer.YesNo.Unknown,
        override val linkedToReOffending: SectionAnswer.YesNo = SectionAnswer.YesNo.Unknown,
        override val questionAnswers: HashMap<String, SectionAnswer> = hashMapOf(),
    ) : NeedSection {
        override val section = Need.ALCOHOL_MISUSE
        override val threshold = Threshold(4, 7)
        override val sanThresholdOverride = Threshold(2, 4)

        @get:JsonIgnore
        val currentUse = questionAnswers["currentUse"] ?: SectionAnswer.Problem.Missing

        @get:JsonIgnore
        val bingeDrinking = questionAnswers["bingeDrinking"] ?: SectionAnswer.Problem.Missing

        @get:JsonIgnore
        val frequencyAndLevel = questionAnswers["frequencyAndLevel"] ?: SectionAnswer.Problem.Missing

        @get:JsonIgnore
        val alcoholTackleMotivation = questionAnswers["alcoholTackleMotivation"] ?: SectionAnswer.Problem.Missing
    }

    data class ThinkingAndBehaviour(
        override val linkedToHarm: SectionAnswer.YesNo = SectionAnswer.YesNo.Unknown,
        override val linkedToReOffending: SectionAnswer.YesNo = SectionAnswer.YesNo.Unknown,
        override val questionAnswers: HashMap<String, SectionAnswer> = hashMapOf(),
        val impulsivity: SectionAnswer.Problem = SectionAnswer.Problem.Missing,
        val temperControl: SectionAnswer.Problem = SectionAnswer.Problem.Missing,
    ) : NeedSection {
        override val section = Need.THINKING_AND_BEHAVIOUR
        override val threshold = Threshold(4, 7)
        override val sanThresholdOverride = Threshold(3, 5)

        @get:JsonIgnore
        val recogniseProblems = questionAnswers["recogniseProblems"] ?: SectionAnswer.Problem.Missing

        @get:JsonIgnore
        val problemSolvingSkills = questionAnswers["problemSolvingSkills"] ?: SectionAnswer.Problem.Missing

        @get:JsonIgnore
        val awarenessOfConsequences = questionAnswers["awarenessOfConsequences"] ?: SectionAnswer.Problem.Missing

        @get:JsonIgnore
        val understandsViewsOfOthers = questionAnswers["understandsViewsOfOthers"] ?: SectionAnswer.Problem.Missing
    }

    data class Attitudes(
        override val linkedToHarm: SectionAnswer.YesNo = SectionAnswer.YesNo.Unknown,
        override val linkedToReOffending: SectionAnswer.YesNo = SectionAnswer.YesNo.Unknown,
        override val questionAnswers: HashMap<String, SectionAnswer> = hashMapOf(),
    ) : NeedSection {
        override val section = Need.ATTITUDES
        override val threshold = Threshold(2, 7)
        override val sanThresholdOverride = Threshold(1, 5)

        @get:JsonIgnore
        val proCriminalAttitudes = questionAnswers["proCriminalAttitudes"] ?: SectionAnswer.Problem.Missing

        @get:JsonIgnore
        val attitudesTowardsSupervision =
            questionAnswers["attitudesTowardsSupervision"] ?: SectionAnswer.Problem.Missing

        @get:JsonIgnore
        val attitudesTowardsCommunitySociety =
            questionAnswers["attitudesTowardsCommunitySociety"] ?: SectionAnswer.Problem.Missing

        @get:JsonIgnore
        val motivationToAddressBehaviour =
            questionAnswers["motivationToAddressBehaviour"] ?: SectionAnswer.Problem.Missing
    }
}