package uk.gov.justice.digital.hmpps.hmppstier.integration.bdd

import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.Parameter
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.communityApiAssessmentsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.custodialNCConvictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.emptyRegistrationsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.femaleOffenderResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.maleOffenderResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.nsisResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithAdditionalFactors
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithMappa
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithRosh
import java.math.BigDecimal

class SetupData constructor(private val communityApi: ClientAndServer) {
  private var outcome: String = "NO_OUTCOME"
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

  fun setNsi(outcome: String) {
    this.outcome = outcome
  }

  fun prepareResponses() {
    communityApiResponse(communityApiAssessmentsResponse(rsr), "/secure/offenders/crn/X12345/assessments")

    when {
      rosh != "NO_ROSH" -> registrations(registrationsResponseWithRosh(rosh))
      mappa != "NO_MAPPA" -> registrations(registrationsResponseWithMappa(mappa))
      additionalFactors.isNotEmpty() -> registrations(registrationsResponseWithAdditionalFactors(additionalFactors))
      else -> registrations(emptyRegistrationsResponse())
    }

    // conviction TODO
    communityApiResponseWithQs(
      custodialNCConvictionResponse(),
      "/secure/offenders/crn/X12345/convictions",
      Parameter("activeOnly", "true")
    )

    if (gender.equals("Male")) {
      communityApiResponse(maleOffenderResponse(), "/secure/offenders/crn/X12345")
    } else {
      communityApiResponse(femaleOffenderResponse(), "/secure/offenders/crn/X12345")
    }
    if (outcome != "NO_OUTCOME") {
      communityApiResponseWithQs(
        nsisResponse(outcome),
        "/secure/offenders/crn/X12345/convictions/\\d+/nsis",
        Parameter("nsiCodes", "BRE,BRES,REC,RECS")
      )
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
    clientAndServer.`when`(HttpRequest.request().withPath(urlTemplate).withQueryStringParameter(qs)).respond(response)

  private fun httpSetup(response: HttpResponse, urlTemplate: String, clientAndServer: ClientAndServer) =
    clientAndServer.`when`(HttpRequest.request().withPath(urlTemplate)).respond(response)

  private fun communityApiResponseWithQs(response: HttpResponse, urlTemplate: String, qs: Parameter) =
    httpSetupWithQs(response, urlTemplate, communityApi, qs)

  private fun communityApiResponse(response: HttpResponse, urlTemplate: String) =
    httpSetup(response, urlTemplate, communityApi)
}
