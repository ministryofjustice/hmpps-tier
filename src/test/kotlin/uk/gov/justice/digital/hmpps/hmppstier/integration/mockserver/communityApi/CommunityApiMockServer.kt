package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.CommunityApiExtension.Companion.communityApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.deliusAssessmentResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.domain.Registration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.registrationResponse
import java.time.LocalDate

class CommunityApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

  companion object {
    lateinit var communityApi: CommunityApiMockServer
  }

  override fun beforeAll(context: ExtensionContext?) {
    communityApi = CommunityApiMockServer()
  }

  override fun beforeEach(context: ExtensionContext?) {
    communityApi.reset()
  }

  override fun afterAll(context: ExtensionContext?) {
    communityApi.stop()
  }
}
class CommunityApiMockServer : ClientAndServer(MOCKSERVER_PORT) {

  companion object {
    private const val MOCKSERVER_PORT = 8091
  }

  fun getRegistrations(crn: String, registrations: List<Registration>) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/registrations").withQueryStringParameter("activeOnly", "true")
    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(registrationResponse(*registrations.toTypedArray())),
    )
  }

  fun getMappaRegistration(crn: String, level: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/registrations").withQueryStringParameter("activeOnly", "true")

    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(registrationResponse(Registration(registerLevel = level))),
    )
  }

  fun getMultipleMappaRegistrations(crn: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/registrations").withQueryStringParameter("activeOnly", "true")

    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(
        registrationResponse(Registration(registerLevel = "M1", startDate = LocalDate.of(2020, 2, 1)), Registration(registerLevel = "M2", startDate = LocalDate.of(2021, 2, 1))),
      ),
    )
  }

  fun getMultipleMappaRegistrationsWithHistoricLatest(crn: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/registrations").withQueryStringParameter("activeOnly", "true")
    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(registrationResponse(Registration(registerLevel = "M2", typeCode = "HREG", startDate = LocalDate.of(2016, 6, 28)), Registration(registerLevel = "M0", startDate = LocalDate.of(2008, 10, 24)))),
    )
  }

  fun getHistoricMappaRegistration(crn: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/registrations").withQueryStringParameter("activeOnly", "true")
    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(registrationResponse(Registration(registerLevel = "M2", typeCode = "HREG"))),
    )
  }

  fun getEmptyRegistration(crn: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/registrations").withQueryStringParameter("activeOnly", "true")
    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(registrationResponse()),
    )
  }

  fun getRoshRegistration(crn: String, typeCode: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/registrations").withQueryStringParameter("activeOnly", "true")
    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(registrationResponse(Registration(typeCode = typeCode))),
    )
  }

  fun getRoshMappaAdditionalFactorsRegistrations(crn: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/registrations").withQueryStringParameter("activeOnly", "true")
    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(registrationResponse(Registration(registerLevel = "M1"), Registration(typeCode = Rosh.HIGH.registerCode), Registration(typeCode = "RCCO"), Registration(typeCode = "RCPR"), Registration(typeCode = "RCHD"))),
    )
  }

  fun getNoLevelRegistration(crn: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/registrations").withQueryStringParameter("activeOnly", "true")
    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(registrationResponse(Registration(typeCode = "STRG"))),
    )
  }

  fun getAssessmentResponse(crn: String, rsr: String = "23.0", ogrs: String = "21") {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/assessments")

    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(deliusAssessmentResponse(rsr, ogrs)),
    )
  }

  fun getEmptyAssessmentResponse(crn: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/assessments")

    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(deliusAssessmentResponse(null, null)),
    )
  }
}
