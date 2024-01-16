package uk.gov.justice.digital.hmpps.hmppstier.integration.bdd

import uk.gov.justice.digital.hmpps.hmppstier.client.AssessmentSummary
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.*
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.arnsApi.ArnsApiExtension.Companion.arnsApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.AssessmentApiExtension.Companion.assessmentApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.response.domain.Answer
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.tierToDeliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Registration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Requirement
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.TierDetails
import java.time.LocalDateTime

class SetupData(
    ids: Map<String, String>,
) {
    private var assessmentDate: LocalDateTime = LocalDateTime.now()
    var crn: String = ids["crn"]!!
    var assessmentId: Long = ids["assessmentId"]!!.toLong()
    private var hasValidAssessment: Boolean = false
    private var currentTier: String = "UD0"
    private var gender: String = "Male"
    private var needs: MutableList<Pair<Need, NeedSeverity>> = mutableListOf()
    private var ogrs: String = "0"
    private var registrations: MutableList<Registration> = mutableListOf()
    private var convictions: MutableList<Conviction> = mutableListOf()
    private var rsr: String = "0"
    private var previousEnforcementActivity: Boolean = false
    private var assessmentAnswers: MutableMap<String, Answer> = mutableMapOf(
        IMPULSIVITY.answerCode to Answer(IMPULSIVITY.answerCode, "Impulsivity", "0"),
        TEMPER_CONTROL.answerCode to Answer(TEMPER_CONTROL.answerCode, "Temper control", "0"),
        PARENTING_RESPONSIBILITIES.answerCode to Answer(
            PARENTING_RESPONSIBILITIES.answerCode,
            "Parental responsibilities",
            "NO"
        ),
    )

    fun setRsr(rsr: String) {
        this.rsr = rsr
    }

    fun setPreviousEnforcementActivity(previousEnforcementActivity: Boolean) {
        this.previousEnforcementActivity = previousEnforcementActivity
    }

    fun addRegistration(registration: Registration) {
        this.registrations.add(registration)
    }

    fun addRequirement(requirement: Requirement) {
        this.convictions[0].requirements.add(requirement)
    }

    fun addConviction(conviction: Conviction) {
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

    fun setCurrentTier(currentTier: String) {
        this.currentTier = currentTier
    }

    fun setGender(gender: String) {
        this.gender = gender
    }

    fun setValidAssessment() {
        this.hasValidAssessment = true
    }

    fun setAssessmentAnswer(question: String, answer: String) {
        setValidAssessment()
        this.assessmentAnswers[question] = Answer(question, this.assessmentAnswers[question]!!.questionText, answer)
    }

    fun setAssessmentDate(date: LocalDateTime) {
        setValidAssessment()
        this.assessmentDate = date
    }

    fun prepareResponses() {
        if (convictions.isEmpty()) {
            addConviction(Conviction())
        }
        tierToDeliusApi.getFullDetails(
            crn,
            TierDetails(gender, currentTier, ogrs, rsr, convictions, registrations, previousEnforcementActivity)
        )
        assessmentsApi()
    }

    private fun assessmentsApi() {
        if (hasValidAssessment) {
            arnsApi.getAssessment(crn, AssessmentSummary(assessmentId, assessmentDate, "LAYER3", "COMPLETE"))

            if (gender == "Female") {
                assessmentApi.getAnswers(assessmentId, assessmentAnswers.values)
            }
            when {
                needs.any() -> arnsApi.getNeeds(crn, needs)
                else -> arnsApi.getNoSeverityNeeds(crn)
            }
        }
    }
}
