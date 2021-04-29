package uk.gov.justice.digital.hmpps.hmppstier.integration.bdd

import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.Parameter
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.communityApiAssessmentsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.custodialAndNonCustodialConvictions
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.custodialNCConvictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.custodialTerminatedConvictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.emptyRegistrationsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.femaleOffenderResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.maleOffenderResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.nsisResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithAdditionalFactors
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithMappa
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithRosh
import java.math.BigDecimal
import java.time.LocalDate

class SetupData constructor(private val communityApi: ClientAndServer) {
  private var convictionTerminated: LocalDate? = null
  private var activeConvictions: Int = 1
  private var outcome: List<String> = emptyList()
  private var gender: String = "Male"
  private var additionalFactors: List<String> = emptyList()
  private var mappa: String = "NO_MAPPA"
  private var rosh: String = "NO_ROSH"
  private var rsr: BigDecimal = BigDecimal(0)

  fun setRsr(rsr: String) {
    this.rsr = BigDecimal(rsr)
  }

  fun setRosh(rosh: String) {
    this.rosh = rosh
  }

  fun setMappa(mappa: String) {
    this.mappa = mappa
  }

  fun setAdditionalFactors(additionalFactors: List<String>) {
    this.additionalFactors = additionalFactors
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

  fun prepareResponses() {
    communityApiResponse(communityApiAssessmentsResponse(rsr), "/secure/offenders/crn/X12345/assessments")

    when {
      rosh != "NO_ROSH" -> registrations(registrationsResponseWithRosh(rosh))
      mappa != "NO_MAPPA" -> registrations(registrationsResponseWithMappa(mappa))
      additionalFactors.isNotEmpty() -> registrations(registrationsResponseWithAdditionalFactors(additionalFactors))
      else -> registrations(emptyRegistrationsResponse())
    }

    if (activeConvictions == 2) {
      communityApiResponseWithQs(
        custodialAndNonCustodialConvictions(),
        "/secure/offenders/crn/X12345/convictions",
        Parameter("activeOnly", "true")
      )
    } else {
      if (null != convictionTerminated) {
        communityApiResponseWithQs(
          custodialTerminatedConvictionResponse(convictionTerminated!!),
          "/secure/offenders/crn/X12345/convictions",
          Parameter("activeOnly", "true")
        )
      } else {
        communityApiResponseWithQs(
          custodialNCConvictionResponse(),
          "/secure/offenders/crn/X12345/convictions",
          Parameter("activeOnly", "true")
        )
      }
    }

    if (gender == "Male") {
      communityApiResponse(maleOffenderResponse(), "/secure/offenders/crn/X12345")
    } else {
      communityApiResponse(femaleOffenderResponse(), "/secure/offenders/crn/X12345")
    }

    if (outcome.isNotEmpty()) {
      outcome.forEach {
        communityApiResponseWithQs(
          nsisResponse(it),
          "/secure/offenders/crn/X12345/convictions/\\d+/nsis",
          Parameter("nsiCodes", "BRE,BRES,REC,RECS")
        )
      }
    }
  }

  private fun registrations(response: HttpResponse) {
    communityApiResponseWithQs(
      response,
      "/secure/offenders/crn/X12345/registrations", Parameter("activeOnly", "true")
    )
  }

  private fun httpSetupWithQs(
    response: HttpResponse,
    urlTemplate: String,
    clientAndServer: ClientAndServer,
    qs: Parameter
  ) =
    clientAndServer.`when`(HttpRequest.request().withPath(urlTemplate).withQueryStringParameter(qs), Times.exactly(1)).respond(response)

  private fun httpSetup(response: HttpResponse, urlTemplate: String, clientAndServer: ClientAndServer) =
    clientAndServer.`when`(HttpRequest.request().withPath(urlTemplate), Times.exactly(1)).respond(response)

  private fun communityApiResponseWithQs(response: HttpResponse, urlTemplate: String, qs: Parameter) =
    httpSetupWithQs(response, urlTemplate, communityApi, qs)

  private fun communityApiResponse(response: HttpResponse, urlTemplate: String) =
    httpSetup(response, urlTemplate, communityApi)
}
