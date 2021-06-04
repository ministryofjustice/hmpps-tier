package uk.gov.justice.digital.hmpps.hmppstier.integration.bdd

import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times.exactly
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse
import org.mockserver.model.Parameter
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.IMPULSIVITY
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.TEMPER_CONTROL
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.assessmentsApiAssessmentsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.assessmentsApiFemaleAnswersResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.assessmentsApiNoSeverityNeedsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.communityApiAssessmentsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.custodialAndNonCustodialConvictions
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.custodialNCConvictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.custodialSCConvictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.custodialTerminatedConvictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.emptyNsisResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.emptyRegistrationsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.femaleOffenderResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.maleOffenderResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.needsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.nonCustodialConvictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.nonRestrictiveRequirementsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.nsisResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithAdditionalFactors
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithMappa
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithMappaAndAdditionalFactors
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithRosh
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithRoshMappaAndAdditionalFactors
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.unpaidWorkRequirementsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.unpaidWorkWithOrderLengthExtendedAndAdditionalHoursRequirementsResponse
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class SetupData(
  private val communityApi: ClientAndServer,
  private val assessmentApi: ClientAndServer,
  ids: Map<String, String>
) {
  private var assessmentDate: LocalDateTime = LocalDateTime.now()
  private var hasNonRestrictiveRequirement: Boolean = false
  private var hasOrderExtended: Boolean = false
  private var hasUnpaidWork: Boolean = false
  private var sentenceType: String = "NC"
  var crn: String = ids["crn"]!!
  var assessmentId: String = ids["assessmentId"]!!
  var convictionId: String = ids["convictionId"]!!
  var secondConvictionId: String = ids["secondConvictionId"]!!
  private var sentenceLengthIndeterminate: Boolean = false
  private var sentenceLength: Long = 1
  private var mainOffence: String = "016"
  private var hasValidAssessment: Boolean = false
  private var convictionTerminatedDate: LocalDate? = null
  private var activeConvictions: Int = 1
  private var outcome: MutableMap<String, String> = mutableMapOf()
  private var gender: String = "Male"
  private var additionalFactors: List<String> = emptyList()
  private var needs: MutableMap<String, String> = mutableMapOf()
  private var mappa: String = "NO_MAPPA"
  private var ogrs: String = "0"
  private var rosh: String = "NO_ROSH"
  private var rsr: BigDecimal = BigDecimal(0)
  private var assessmentAnswers: MutableMap<String, String> = mutableMapOf(
    IMPULSIVITY.answerCode to "0",
    TEMPER_CONTROL.answerCode to "0",
    PARENTING_RESPONSIBILITIES.answerCode to "NO"
  )

  fun setRsr(rsr: String) {
    this.rsr = BigDecimal(rsr)
  }

  fun setRosh(rosh: String) {
    this.rosh = rosh
  }

  fun setMappa(mappa: String) {
    this.mappa = mappa
  }

  fun setOgrs(ogrs: String) {
    setValidAssessment() // There needs to be a valid assessment to access ogrs code path
    this.ogrs = ogrs
  }

  fun setAdditionalFactors(additionalFactors: List<String>) {
    setValidAssessment() // for IOM
    this.additionalFactors = additionalFactors
  }

  fun setNeeds(needs: Map<String, String>) {
    setValidAssessment() // There needs to be a valid assessment to access needs code path
    this.needs.putAll(needs)
  }

  fun addNeed(need: String, severity: String) {
    setNeeds(mapOf(need to severity))
  }

  fun setGender(gender: String) {
    this.gender = gender
  }

  fun setNsiOutcome(outcome: String, conviction: String) {
    this.outcome[conviction] = outcome
  }

  fun setTwoActiveConvictions() {
    this.activeConvictions = 2
  }

  fun setConvictionTerminatedDate(convictionTerminated: LocalDate) {
    this.convictionTerminatedDate = convictionTerminated
  }

  fun setValidAssessment() {
    this.hasValidAssessment = true
  }

  fun setAssessmentAnswer(question: String, answer: String) {
    setValidAssessment()
    this.assessmentAnswers[question] = answer
  }

  fun setMainOffenceAbstractingElectricity() {
    this.mainOffence = "043"
  }

  fun setSentenceLength(months: Long) {
    this.sentenceLength = months
  }

  fun setSentenceLengthIndeterminate() {
    this.sentenceLengthIndeterminate = true
  }

  fun setSentenceType(sentenceType: String) {
    this.sentenceType = sentenceType
  }

  fun setUnpaidWork() {
    this.hasUnpaidWork = true
  }

  fun setOrderExtended() {
    this.hasOrderExtended = true
  }

  fun setNonRestrictiveRequirement() {
    this.hasNonRestrictiveRequirement = true
  }

  fun setAssessmentDate(date: LocalDateTime) {
    setValidAssessment()
    this.assessmentDate = date
  }

  fun prepareResponses() {
    communityApiResponse(communityApiAssessmentsResponse(rsr, ogrs), "/secure/offenders/crn/$crn/assessments")
    registrations(RegistrationsSetup(rosh, mappa, additionalFactors))
    assessmentsApi()
    convictions()
    requirements()

    when (gender) {
      "Male" -> communityApiResponse(maleOffenderResponse(), "/secure/offenders/crn/$crn")
      else -> femaleWithBreachAndRecall()
    }
  }

  private fun femaleWithBreachAndRecall() {
    communityApiResponse(femaleOffenderResponse(), "/secure/offenders/crn/$crn")
    if (outcome.isNotEmpty()) {
      outcome.forEach {
        breachAndRecall(nsisResponse(it.value), it.key)
      }
    } else {
      breachAndRecall(emptyNsisResponse(), convictionId)
    }
  }

  private fun requirements() {
    when {
      hasOrderExtended && hasUnpaidWork -> requirements(
        unpaidWorkWithOrderLengthExtendedAndAdditionalHoursRequirementsResponse()
      )
      hasUnpaidWork -> requirements(unpaidWorkRequirementsResponse())
      hasNonRestrictiveRequirement -> requirements(nonRestrictiveRequirementsResponse())
    }
  }

  private fun convictions() =
    if (activeConvictions == 2) {
      convictions(custodialAndNonCustodialConvictions(convictionId, secondConvictionId))
    } else {
      if (null != convictionTerminatedDate) {
        convictions(custodialTerminatedConvictionResponse(convictionTerminatedDate!!, convictionId))
      } else {
        when {
          sentenceLengthIndeterminate -> convictions(
            custodialNCConvictionResponse(mainOffence, courtAppearanceOutcome = "303", convictionId = convictionId)
          )
          sentenceType == "SC" -> convictions(custodialSCConvictionResponse(convictionId))
          sentenceType == "NC" -> convictions(custodialNCConvictionResponse(mainOffence, sentenceLength, convictionId = convictionId))
          else -> convictions(nonCustodialConvictionResponse(convictionId))
        }
      }
    }

  private fun assessmentsApi() {
    if (hasValidAssessment) {
      assessmentApiResponse(
        assessmentsApiAssessmentsResponse(assessmentDate, assessmentId),
        "/offenders/crn/$crn/assessments/summary"
      )
      if (gender == "Female") {
        assessmentApiResponse(
          assessmentsApiFemaleAnswersResponse(assessmentAnswers, assessmentId),
          "/assessments/oasysSetId/$assessmentId/answers"
        )
      }
      assessmentApiResponse(
        when {
          needs.any() -> needsResponse(needs)
          else -> assessmentsApiNoSeverityNeedsResponse()
        },
        "/assessments/oasysSetId/$assessmentId/needs"
      )
    }
  }

  private fun registrations(registrationsSetup: RegistrationsSetup) =
    when {
      registrationsSetup.allRegistrationsPresent() -> registrations(
        registrationsResponseWithRoshMappaAndAdditionalFactors(
          rosh,
          mappa,
          additionalFactors
        )
      )
      registrationsSetup.mappaAndAdditionalFactors() -> registrations(
        registrationsResponseWithMappaAndAdditionalFactors(
          mappa,
          additionalFactors
        )
      )
      registrationsSetup.hasRosh() -> registrations(registrationsResponseWithRosh(rosh))
      registrationsSetup.hasMappa() -> registrations(registrationsResponseWithMappa(mappa))
      registrationsSetup.hasAdditionalFactors() -> registrations(
        registrationsResponseWithAdditionalFactors(
          additionalFactors
        )
      )
      else -> registrations(emptyRegistrationsResponse())
    }

  private fun breachAndRecall(response: HttpResponse, convictionId: String) =
    communityApiResponseWithQs(
      response,
      "/secure/offenders/crn/$crn/convictions/$convictionId/nsis",
      Parameter("nsiCodes", "BRE,BRES,REC,RECS")
    )

  private fun convictions(response: HttpResponse) =
    communityApiResponseWithQs(
      response,
      "/secure/offenders/crn/$crn/convictions", Parameter("activeOnly", "true")
    )

  private fun registrations(response: HttpResponse) =
    communityApiResponseWithQs(
      response,
      "/secure/offenders/crn/$crn/registrations", Parameter("activeOnly", "true")
    )

  private fun requirements(response: HttpResponse) =
    communityApiResponseWithQs(
      response,
      "/secure/offenders/crn/$crn/convictions/$convictionId/requirements", Parameter("activeOnly", "true")
    )

  private fun httpSetup(response: HttpResponse, urlTemplate: String, api: ClientAndServer) =
    api.`when`(request().withPath(urlTemplate), exactly(1)).respond(response)

  private fun communityApiResponseWithQs(response: HttpResponse, urlTemplate: String, qs: Parameter) =
    communityApi.`when`(request().withPath(urlTemplate).withQueryStringParameter(qs), exactly(1))
      .respond(response)

  private fun communityApiResponse(response: HttpResponse, urlTemplate: String) =
    httpSetup(response, urlTemplate, communityApi)

  private fun assessmentApiResponse(response: HttpResponse, urlTemplate: String) =
    httpSetup(response, urlTemplate, assessmentApi)
}

class RegistrationsSetup(private val rosh: String, private val mappa: String, private val additionalFactors: List<String>) {
  fun allRegistrationsPresent() = hasRosh() && hasMappa() && hasAdditionalFactors()
  fun mappaAndAdditionalFactors() = hasMappa() && hasAdditionalFactors()
  fun hasRosh() = rosh != "NO_ROSH"
  fun hasMappa() = mappa != "NO_MAPPA"
  fun hasAdditionalFactors() = additionalFactors.isNotEmpty()
}
