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
import uk.gov.justice.digital.hmpps.hmppstier.client.*
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

    override fun beforeAll(context: ExtensionContext?) {
        arnsApi = ArnsApiMockServer()
    }

    override fun beforeEach(context: ExtensionContext?) {
        arnsApi.reset()
    }

    override fun afterAll(context: ExtensionContext?) {
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
        additionalFactors: Map<AdditionalFactorForWomen, SectionAnswer> = mapOf()
    ) {
        val request = HttpRequest.request().withPath("/tier-assessment/sections/$crn")
        val need = needs.toMap()
        val response = AssessmentForTier(
            assessment = AssessmentSummary(assessmentId ?: 0, LocalDateTime.now().minusDays(30), "LAYER3", "COMPLETE"),
            accommodation = need[ACCOMMODATION]?.let { NeedSection.Accommodation(it) },
            educationTrainingEmployment = need[EDUCATION_TRAINING_AND_EMPLOYABILITY]?.let {
                NeedSection.EducationTrainingEmployability(it)
            },
            relationships = (need[RELATIONSHIPS] ?: NeedSeverity.NO_NEED).let {
                NeedSection.Relationships(
                    it,
                    additionalFactors[AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES] as SectionAnswer.YesNo?
                        ?: SectionAnswer.YesNo.No
                )
            },
            lifestyleAndAssociates = need[LIFESTYLE_AND_ASSOCIATES]?.let { NeedSection.LifestyleAndAssociates(it) },
            drugMisuse = need[DRUG_MISUSE]?.let { NeedSection.DrugMisuse(it) },
            alcoholMisuse = need[ALCOHOL_MISUSE]?.let { NeedSection.AlcoholMisuse(it) },
            thinkingAndBehaviour = (need[THINKING_AND_BEHAVIOUR] ?: NeedSeverity.NO_NEED).let {
                NeedSection.ThinkingAndBehaviour(
                    it,
                    additionalFactors.problem(AdditionalFactorForWomen.IMPULSIVITY),
                    additionalFactors.problem(AdditionalFactorForWomen.TEMPER_CONTROL)
                )
            },
            attitudes = need[ATTITUDES]?.let { NeedSection.Attitudes(it) }
        )
        arnsApi.`when`(request, Times.exactly(1)).respond(
            HttpResponse.response().withContentType(MediaType.APPLICATION_JSON)
                .withBody(
                    objectMapper().writeValueAsString(response)
                )
        )
    }

    private fun Map<AdditionalFactorForWomen, SectionAnswer>.problem(additionFactor: AdditionalFactorForWomen) =
        this[additionFactor] as SectionAnswer.Problem? ?: SectionAnswer.Problem.None

    fun getNotFoundAssessment(crn: String) {
        val request = HttpRequest.request().withPath("/tier-assessment/sections/$crn")
        arnsApi.`when`(request, Times.exactly(1))
            .respond(HttpResponse.notFoundResponse().withContentType(MediaType.APPLICATION_JSON))
    }
}
