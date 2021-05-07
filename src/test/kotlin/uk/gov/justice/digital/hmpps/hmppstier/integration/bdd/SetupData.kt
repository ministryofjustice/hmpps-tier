package uk.gov.justice.digital.hmpps.hmppstier.integration.bdd

import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times.exactly
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse
import org.mockserver.model.Parameter
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.IMPULSIVITY
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.TEMPER_CONTROL
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.OffenceCode
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
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.nsisResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithAdditionalFactors
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithMappa
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithMappaAndAdditionalFactors
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithRosh
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithRoshMappaAndAdditionalFactors
import java.math.BigDecimal
import java.time.LocalDate

class SetupData(
  private val communityApi: ClientAndServer,
  private val assessmentApi: ClientAndServer,
  ids: Map<String, String>
) {
  private var sentenceType: String = "NC"
  var crn: String = ids["crn"]!!
  var assessmentId: String = ids["assessmentId"]!!
  private var sentenceLengthIndeterminate: Boolean = false
  private var sentenceLength: Long = 1
  private var mainOffence: String = "016"
  private var hasValidAssessment: Boolean = false
  private var convictionTerminated: LocalDate? = null
  private var activeConvictions: Int = 1
  private var outcome: List<String> = emptyList()
  private var gender: String = "Male"
  private var additionalFactors: List<String> = emptyList()
  private var needs: MutableMap<String, String> = mutableMapOf()
  private var mappa: String = "NO_MAPPA"
  private var ogrs: String = "0"
  private var rosh: String = "NO_ROSH"
  private var rsr: BigDecimal = BigDecimal(0)
  private var assessmentAnswers: MutableMap<String, String> = mutableMapOf(
    Pair(IMPULSIVITY.answerCode, "0"),
    Pair(TEMPER_CONTROL.answerCode, "0"),
    Pair(PARENTING_RESPONSIBILITIES.answerCode, "NO")
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
    this.hasValidAssessment = true // There needs to be a valid assessment to access ogrs code path
    this.ogrs = ogrs
  }

  fun setAdditionalFactors(additionalFactors: List<String>) {
    this.hasValidAssessment = true // for IOM
    this.additionalFactors = additionalFactors
  }

  fun setNeeds(needs: Map<String, String>) {
    this.hasValidAssessment = true // There needs to be a valid assessment to access needs code path
    this.needs.putAll(needs)
  }

  fun addNeed(need: String, severity: String) {
    setNeeds(mapOf(need to severity))
  }

  fun setGender(gender: String) {
    this.gender = gender
  }

  fun setNsiOutcomes(outcome: List<String>) {
    this.outcome = outcome
  }

  fun setTwoActiveConvictions() {
    this.activeConvictions = 2
  }

  fun setConvictionTerminatedDate(convictionTerminated: LocalDate) {
    this.convictionTerminated = convictionTerminated
  }

  fun setValidAssessment() {
    this.hasValidAssessment = true
  }

  fun setAssessmentAnswer(question: String, answer: String) {
    this.assessmentAnswers[question] = answer
  }

  fun setMainOffenceArson() {
    this.mainOffence = OffenceCode._056.code
  }

  fun setMainOffenceViolence() {
    this.mainOffence = OffenceCode._001.code
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


  fun prepareResponses() {
    communityApiResponse(communityApiAssessmentsResponse(rsr, ogrs), "/secure/offenders/crn/$crn/assessments")

    when {
      rosh != "NO_ROSH" &&
        mappa != "NO_MAPPA" &&
        additionalFactors.isNotEmpty() -> registrations(
        registrationsResponseWithRoshMappaAndAdditionalFactors(
          rosh,
          mappa,
          additionalFactors
        )
      )
      mappa != "NO_MAPPA" &&
        additionalFactors.isNotEmpty() -> registrations(
        registrationsResponseWithMappaAndAdditionalFactors(
          mappa,
          additionalFactors
        )
      )
      rosh != "NO_ROSH" -> registrations(registrationsResponseWithRosh(rosh))
      mappa != "NO_MAPPA" -> registrations(registrationsResponseWithMappa(mappa))
      additionalFactors.isNotEmpty() -> registrations(registrationsResponseWithAdditionalFactors(additionalFactors))
      else -> registrations(emptyRegistrationsResponse())
    }

    if (hasValidAssessment) {
      assessmentApiResponse(
        assessmentsApiAssessmentsResponse(LocalDate.now().year, assessmentId),
        "/offenders/crn/$crn/assessments/summary"
      )
      if (gender == "Female") {
        assessmentApiResponse(
          assessmentsApiFemaleAnswersResponse(assessmentAnswers, assessmentId),
          "/assessments/oasysSetId/$assessmentId/answers"
        )
      }
      assessmentApiResponse(
        if (needs.any()) {
          needsResponse(needs)
        } else {
          assessmentsApiNoSeverityNeedsResponse()
        },
        "/assessments/oasysSetId/$assessmentId/needs"
      )
    }

    if (activeConvictions == 2) {
      convictions(custodialAndNonCustodialConvictions())
    } else {
      if (null != convictionTerminated) {
        convictions(custodialTerminatedConvictionResponse(convictionTerminated!!))
      } else {
        if (sentenceLengthIndeterminate) {
          convictions(custodialNCConvictionResponse(mainOffence, courtAppearanceOutcome = "303"))
        } else {
          if (sentenceType == "SC") {
            convictions(custodialSCConvictionResponse())
          } else {
            convictions(custodialNCConvictionResponse(mainOffence, sentenceLength))
          }
        }
      }
    }

    when (gender) {
      "Male" -> communityApiResponse(maleOffenderResponse(), "/secure/offenders/crn/$crn")
      else -> communityApiResponse(femaleOffenderResponse(), "/secure/offenders/crn/$crn")
    }

    if (gender == "Female") {
      if (outcome.isNotEmpty()) {
        outcome.forEach {
          breachAndRecall(nsisResponse(it))
        }
      } else {
        breachAndRecall(emptyNsisResponse())
      }
    }
  }

  private fun breachAndRecall(response: HttpResponse) {
    communityApiResponseWithQs(
      response,
      "/secure/offenders/crn/$crn/convictions/\\d+/nsis", // should supply the actual convictionID here?
      Parameter("nsiCodes", "BRE,BRES,REC,RECS")
    )
  }

  private fun convictions(response: HttpResponse) {
    communityApiResponseWithQs(
      response,
      "/secure/offenders/crn/$crn/convictions",
      Parameter("activeOnly", "true")
    )
  }

  private fun assessmentApiResponse(response: HttpResponse, urlTemplate: String) {
    httpSetup(response, urlTemplate, assessmentApi)
  }

  private fun registrations(response: HttpResponse) {
    communityApiResponseWithQs(
      response,
      "/secure/offenders/crn/$crn/registrations", Parameter("activeOnly", "true")
    )
  }

  private fun httpSetup(response: HttpResponse, urlTemplate: String, api: ClientAndServer) =
    api.`when`(request().withPath(urlTemplate), exactly(1)).respond(response)

  private fun communityApiResponseWithQs(response: HttpResponse, urlTemplate: String, qs: Parameter) =
    communityApi.`when`(request().withPath(urlTemplate).withQueryStringParameter(qs), exactly(1))
      .respond(response)

  private fun communityApiResponse(response: HttpResponse, urlTemplate: String) =
    httpSetup(response, urlTemplate, communityApi)

}
