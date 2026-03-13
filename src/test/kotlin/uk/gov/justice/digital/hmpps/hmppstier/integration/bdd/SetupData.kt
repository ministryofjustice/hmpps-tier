package uk.gov.justice.digital.hmpps.hmppstier.integration.bdd

import uk.gov.justice.digital.hmpps.hmppstier.client.arns.SectionAnswer
import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusConviction
import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusRegistration
import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusRequirement
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.*
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.arnsApi.ArnsApiExtension.Companion.arnsApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusConviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.ResponseGenerator.deliusResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.deliusApi
import java.time.LocalDateTime

class SetupData(
    ids: Map<String, String>,
) {
    private var assessmentDate: LocalDateTime = LocalDateTime.now()
    var crn: String = ids["crn"]!!
    var assessmentId: Long = ids["assessmentId"]!!.toLong()
    private var hasValidAssessment: Boolean = false
    private var gender: String = "Male"
    private var needs: MutableList<Pair<Need, NeedSeverity>> = mutableListOf()
    private var ogrs: String = "0"
    private var registrations: MutableList<DeliusRegistration> = mutableListOf()
    private var convictions: MutableList<DeliusConviction> = mutableListOf()
    private var rsr: String = "0"
    private var previousEnforcementActivity: Boolean = false
    private var assessmentAnswers: MutableMap<AdditionalFactorForWomen, SectionAnswer> = mutableMapOf(
        IMPULSIVITY to SectionAnswer.Problem.None,
        TEMPER_CONTROL to SectionAnswer.Problem.None,
        PARENTING_RESPONSIBILITIES to SectionAnswer.YesNo.No
    )

    fun setRsr(rsr: String) {
        this.rsr = rsr
    }

    fun setPreviousEnforcementActivity(previousEnforcementActivity: Boolean) {
        this.previousEnforcementActivity = previousEnforcementActivity
    }

    fun addRegistration(registration: DeliusRegistration) {
        this.registrations.add(registration)
    }

    fun addRequirement(requirement: DeliusRequirement) {
        val conviction = convictions.first()
        convictions[0] = conviction.copy(requirements = conviction.requirements + requirement)
    }

    fun addConviction(conviction: DeliusConviction) {
        this.convictions.add(conviction)
    }

    fun setOgrs(ogrs: String) {
        setValidAssessment() // There needs to be a valid assessment to access ogrs code path
        this.ogrs = ogrs
    }

    fun setNeeds(vararg needs: Pair<Need, NeedSeverity>) {
        setValidAssessment() // There needs to be a valid assessment to access needs code path
        this.needs.addAll(needs.toList())
    }

    fun setGender(gender: String) {
        this.gender = gender
    }

    fun setValidAssessment() {
        this.hasValidAssessment = true
    }

    fun setAssessmentAnswer(question: String, answer: String) {
        setValidAssessment()
        val additionalFactor: AdditionalFactorForWomen = checkNotNull(AdditionalFactorForWomen.valueOf(question))
        val sectionAnswer: SectionAnswer = when (answer) {
            "YES" -> SectionAnswer.YesNo.Yes
            "NO" -> SectionAnswer.YesNo.No
            "0" -> SectionAnswer.Problem.None
            "1" -> SectionAnswer.Problem.Some
            "2" -> SectionAnswer.Problem.Significant
            else -> SectionAnswer.YesNo.Unknown
        }
        this.assessmentAnswers[additionalFactor] = sectionAnswer
    }

    fun setAssessmentDate(date: LocalDateTime) {
        setValidAssessment()
        this.assessmentDate = date
    }

    fun prepareResponses() {
        if (convictions.isEmpty()) {
            addConviction(deliusConviction())
        }
        deliusApi.getFullDetails(
            crn,
            deliusResponse(gender, ogrs, rsr, registrations, convictions, previousEnforcementActivity),
        )
        assessmentsApi()
    }

    private fun assessmentsApi() {
        if (hasValidAssessment) {
            arnsApi.getTierAssessmentDetails(
                crn,
                assessmentId,
                needs.toMap(),
                assessmentAnswers
            )
        }
    }
}
