package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.arnsApi

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
import uk.gov.justice.digital.hmpps.hmppstier.client.AssessmentForTier
import uk.gov.justice.digital.hmpps.hmppstier.client.AssessmentSummary
import uk.gov.justice.digital.hmpps.hmppstier.client.NeedSection
import uk.gov.justice.digital.hmpps.hmppstier.client.SectionAnswer
import uk.gov.justice.digital.hmpps.hmppstier.client.SectionAnswer.Problem
import uk.gov.justice.digital.hmpps.hmppstier.client.SectionAnswer.YesNo
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need.*
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.arnsApi.ArnsApiExtension.Companion.arnsApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.objectMapper
import java.time.LocalDateTime

class ArnsApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    companion object {
        lateinit var arnsApi: ArnsApiMockServer
    }

    override fun beforeAll(context: ExtensionContext) = start()
    override fun beforeEach(context: ExtensionContext) = reset()
    override fun afterAll(context: ExtensionContext) = stop()

    fun start() {
        arnsApi = ArnsApiMockServer()
    }

    fun reset() {
        arnsApi.reset()
    }

    fun stop() {
        arnsApi.stop()
    }
}

class ArnsApiMockServer : ClientAndServer(MOCKSERVER_PORT) {

    companion object {
        private const val MOCKSERVER_PORT = 8094
    }

    fun getTierAssessmentDetails(
        crn: String,
        assessmentId: Long? = null,
        needs: Map<Need, NeedSeverity> = Need.entries.associateWith { NeedSeverity.NO_NEED },
        additionalFactors: Map<AdditionalFactorForWomen, SectionAnswer> = mapOf(),
        sanIndicator: Boolean = false,
    ) {
        val request = HttpRequest.request().withPath("/tier-assessment/sections/$crn")
        val need = needs.toMap()
        val response = AssessmentForTier(
            assessment = AssessmentSummary(
                assessmentId ?: 0,
                LocalDateTime.now().minusDays(30),
                "LAYER3",
                "COMPLETE",
                sanIndicator
            ),
            accommodation = need[ACCOMMODATION]?.let { accommodation(it, sanIndicator) },
            educationTrainingEmployability = need[EDUCATION_TRAINING_AND_EMPLOYABILITY]?.let { ete(it) },
            relationships = relationships(
                (need[RELATIONSHIPS] ?: NeedSeverity.NO_NEED),
                additionalFactors[AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES] as YesNo? ?: YesNo.No
            ),
            lifestyleAndAssociates = need[LIFESTYLE_AND_ASSOCIATES]?.let { lifestyle(it) },
            drugMisuse = need[DRUG_MISUSE]?.let { drugMisuse(it, sanIndicator) },
            alcoholMisuse = need[ALCOHOL_MISUSE]?.let { alcoholMisuse(it, sanIndicator) },
            thinkingAndBehaviour = thinkingAndBehaviour(
                (need[THINKING_AND_BEHAVIOUR] ?: NeedSeverity.NO_NEED),
                additionalFactors.problem(AdditionalFactorForWomen.IMPULSIVITY),
                additionalFactors.problem(AdditionalFactorForWomen.TEMPER_CONTROL),
                sanIndicator
            ),
            attitudes = need[ATTITUDES]?.let { attitudes(it) }
        )
        val json = objectMapper().writeValueAsString(response)
        arnsApi.`when`(request, Times.exactly(1)).respond(
            HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(json)
        )
    }

    private fun Map<AdditionalFactorForWomen, SectionAnswer>.problem(additionFactor: AdditionalFactorForWomen) =
        this[additionFactor] as Problem? ?: Problem.None

    fun getNotFoundAssessment(crn: String) {
        val request = HttpRequest.request().withPath("/tier-assessment/sections/$crn")
        arnsApi.`when`(request, Times.exactly(1))
            .respond(HttpResponse.notFoundResponse().withContentType(MediaType.APPLICATION_JSON))
    }

    private fun accommodation(severity: NeedSeverity, sanIndicator: Boolean): NeedSection.Accommodation? {
        val questions = listOf("suitabilityOfAccommodation", "permanenceOfAccommodation", "locationOfAccommodation")
        val noFixedAbode = if (severity == NeedSeverity.NO_NEED || sanIndicator) YesNo.No else YesNo.Yes
        val questionAnswers = when (severity) {
            NeedSeverity.NO_NEED -> questions.map { it to Problem.None }
            NeedSeverity.STANDARD -> questions.map { it to Problem.Some }
            NeedSeverity.SEVERE -> questions.map { it to Problem.Significant }
        }.toMap() + ("noFixedAbodeOrTransient" to noFixedAbode)
        return NeedSection.Accommodation(YesNo.No, YesNo.No, HashMap(questionAnswers))
    }

    private fun ete(severity: NeedSeverity): NeedSection.EducationTrainingEmployability? {
        val questions = listOf("unemployed", "employmentHistory", "workRelatedSkills", "attitudeToEmployment")
        val questionAnswers = when (severity) {
            NeedSeverity.NO_NEED -> questions.map { it to Problem.None }
            NeedSeverity.STANDARD -> questions.map { it to Problem.Some }
            NeedSeverity.SEVERE -> questions.map { it to Problem.Significant }
        }.toMap()
        return NeedSection.EducationTrainingEmployability(YesNo.No, YesNo.No, HashMap(questionAnswers))
    }

    private fun relationships(severity: NeedSeverity, parentalResponsibilities: YesNo): NeedSection.Relationships? {
        val questions = listOf("relCloseFamily", "experienceOfChildhood", "prevCloseRelationships")
        val questionAnswers = when (severity) {
            NeedSeverity.NO_NEED -> questions.map { it to Problem.None }
            NeedSeverity.STANDARD -> questions.map { it to Problem.Some }
            NeedSeverity.SEVERE -> questions.map { it to Problem.Significant }
        }.toMap()
        return NeedSection.Relationships(YesNo.No, YesNo.No, HashMap(questionAnswers), parentalResponsibilities)
    }

    private fun lifestyle(severity: NeedSeverity): NeedSection.LifestyleAndAssociates? {
        val questions = listOf("regActivitiesEncourageOffending", "easilyInfluenced", "recklessness")
        val questionAnswers = when (severity) {
            NeedSeverity.NO_NEED -> questions.map { it to Problem.None }
            NeedSeverity.STANDARD -> questions.map { it to Problem.Some }
            NeedSeverity.SEVERE -> questions.map { it to Problem.Significant }
        }.toMap()
        return NeedSection.LifestyleAndAssociates(YesNo.No, YesNo.No, HashMap(questionAnswers))
    }

    private fun drugMisuse(severity: NeedSeverity, sanIndicator: Boolean): NeedSection.DrugMisuse? {
        val questions = listOf("currentDrugNoted", "motivationToTackleDrugMisuse", "drugsMajorActivity")
        val injected: YesNo = if (severity == NeedSeverity.NO_NEED || sanIndicator) YesNo.No else YesNo.Yes
        val questionAnswers = when (severity) {
            NeedSeverity.NO_NEED -> questions.map { it to Problem.None }
            NeedSeverity.STANDARD -> questions.mapIndexed { i, q -> q to if (sanIndicator && i % 2 == 0) Problem.None else Problem.Some }
            NeedSeverity.SEVERE -> questions.map { it to Problem.Significant }
        }.toMap() + ("everInjectedDrugs" to injected)
        return NeedSection.DrugMisuse(YesNo.No, YesNo.No, HashMap(questionAnswers))
    }

    private fun alcoholMisuse(severity: NeedSeverity, sanIndicator: Boolean): NeedSection.AlcoholMisuse? {
        val questions = listOf("currentUse", "bingeDrinking", "frequencyAndLevel", "alcoholTackleMotivation")
        val questionAnswers = when (severity) {
            NeedSeverity.NO_NEED -> questions.map { it to Problem.None }
            NeedSeverity.STANDARD -> questions.mapIndexed { i, q -> q to if (sanIndicator && i % 2 == 0) Problem.None else Problem.Some }
            NeedSeverity.SEVERE -> questions.mapIndexed { i, q -> q to if (sanIndicator && i % 2 == 0) Problem.None else Problem.Significant }
        }.toMap()
        return NeedSection.AlcoholMisuse(YesNo.No, YesNo.No, HashMap(questionAnswers))
    }

    private fun thinkingAndBehaviour(
        severity: NeedSeverity,
        impulsivity: Problem,
        temperControl: Problem,
        sanIndicator: Boolean
    ): NeedSection.ThinkingAndBehaviour? {
        val questions =
            listOf("recogniseProblems", "problemSolvingSkills", "awarenessOfConsequences", "understandsViewsOfOthers")
        val questionAnswers = when (severity) {
            NeedSeverity.NO_NEED -> questions.map { it to Problem.None }
            NeedSeverity.STANDARD -> questions.mapIndexed { i, q -> q to if (sanIndicator && i == 0) Problem.None else Problem.Some }
            NeedSeverity.SEVERE -> questions.mapIndexed { i, q -> q to if (sanIndicator && i != 0) Problem.Some else Problem.Significant }
        }.toMap()
        return NeedSection.ThinkingAndBehaviour(YesNo.No, YesNo.No, HashMap(questionAnswers), impulsivity, temperControl)
    }

    private fun attitudes(severity: NeedSeverity): NeedSection.Attitudes? {
        val questions = listOf(
            "proCriminalAttitudes",
            "attitudesTowardsSupervision",
            "attitudesTowardsCommunitySociety",
            "motivationToAddressBehaviour"
        )
        val questionAnswers = when (severity) {
            NeedSeverity.NO_NEED -> questions.map { it to Problem.None }
            NeedSeverity.STANDARD -> questions.map { it to Problem.Some }
            NeedSeverity.SEVERE -> questions.map { it to Problem.Significant }
        }.toMap()
        return NeedSection.Attitudes(YesNo.No, YesNo.No, HashMap(questionAnswers))
    }
}
