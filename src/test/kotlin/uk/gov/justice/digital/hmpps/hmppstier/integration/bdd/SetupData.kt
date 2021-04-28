package uk.gov.justice.digital.hmpps.hmppstier.integration.bdd

import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.Parameter
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.communityApiAssessmentsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.custodialNCConvictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.emptyRegistrationsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.maleOffenderResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithAdditionalFactors
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithMappa
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithRosh
import java.math.BigDecimal

class SetupData constructor (private val communityApi: ClientAndServer) {
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

  fun prepareResponses() {
    // RSR BDD
    communityApiResponse(communityApiAssessmentsResponse(rsr), "/secure/offenders/crn/X12345/assessments")

    when {
      // ROSH BDD
      rosh != "NO_ROSH" -> communityApiResponseWithQs(
        registrationsResponseWithRosh(rosh),
        "/secure/offenders/crn/X12345/registrations", Parameter("activeOnly", "true")
      )

      // MAPPA BDD
      mappa != "NO_MAPPA" -> communityApiResponseWithQs(
        registrationsResponseWithMappa(mappa),
        "/secure/offenders/crn/X12345/registrations", Parameter("activeOnly", "true")
      )
      // additional factors BDD
      additionalFactors.isNotEmpty() -> communityApiResponseWithQs(
        registrationsResponseWithAdditionalFactors(additionalFactors),
        "/secure/offenders/crn/X12345/registrations", Parameter("activeOnly", "true")
      )
      else -> communityApiResponseWithQs(emptyRegistrationsResponse(), "/secure/offenders/crn/X12345/registrations", Parameter("activeOnly", "true"))
    }
    // conviction TODO
    communityApiResponseWithQs(custodialNCConvictionResponse(), "/secure/offenders/crn/X12345/convictions", Parameter("activeOnly", "true"))
    // offender TODO
    communityApiResponse(maleOffenderResponse(), "/secure/offenders/crn/X12345")
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
