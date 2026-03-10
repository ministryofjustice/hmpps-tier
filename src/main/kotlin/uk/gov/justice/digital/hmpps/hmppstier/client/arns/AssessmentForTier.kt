package uk.gov.justice.digital.hmpps.hmppstier.client.arns

import java.io.Serializable

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