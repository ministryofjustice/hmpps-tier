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
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.CommunityApiExtension.Companion.communityApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.convictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.deliusAssessmentResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.emptyRegistrationResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.nsiResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.offenderResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.registrationResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.requirementResponse

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

  fun getAssessmentResponse(crn1: String, crn2: String? = null, rsr: String = "23.0", ogrs: String = "21") {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn1/assessments")

    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(deliusAssessmentResponse(rsr, ogrs)),
    )
    if (crn2 != null) {
      val request2 = HttpRequest.request().withPath("/secure/offenders/crn/$crn2/assessments")

      communityApi.`when`(request2, Times.exactly(1)).respond(
        HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(deliusAssessmentResponse(rsr, ogrs)),
      )
    }
  }

  fun getEmptyAssessmentResponse(crn: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/assessments")

    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(deliusAssessmentResponse(null, null)),
    )
  }

  fun getOffender(crn1: String, crn2: String? = null, gender: String, currentTier: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn1/all")

    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(offenderResponse(gender, currentTier)),
    )
    if (crn2 != null) {
      val request2 = HttpRequest.request().withPath("/secure/offenders/crn/$crn2/all")

      communityApi.`when`(request2, Times.exactly(1)).respond(
        HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(offenderResponse(gender, currentTier)),
      )
    }
  }

  fun getNotFoundOffender(crn: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/all")

    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.notFoundResponse(),
    )
  }

  fun getConviction(crn1: String, crn2: String? = null, convictionId: String? = "12345") {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn1/convictions")

    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(convictionResponse(convictionId)),
    )

    if (crn2 != null) {
      val request2 = HttpRequest.request().withPath("/secure/offenders/crn/$crn2/convictions")

      communityApi.`when`(request2, Times.exactly(1)).respond(
        HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(convictionResponse(convictionId)),
      )
    }
  }

  fun getNotFoundConviction(crn: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/convictions")

    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.notFoundResponse(),
    )
  }

  fun getRequirement(crn1: String, crn2: String? = null, convictionId: Long = 12345) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn1/convictions/$convictionId/requirements")

    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(requirementResponse()),
    )

    if (crn2 != null) {
      val request2 = HttpRequest.request().withPath("/secure/offenders/crn/$crn2/convictions/$convictionId/requirements")

      communityApi.`when`(request2, Times.exactly(1)).respond(
        HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(requirementResponse()),
      )
    }
  }

  fun getNotFoundRequirement(crn: String, convictionId: Long = 12345) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/convictions/$convictionId/requirements")

    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.notFoundResponse(),
    )
  }

  fun getRegistration(crn1: String, crn2: String? = null) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn1/registrations")

    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(registrationResponse()),
    )

    if (crn2 != null) {
      val request2 = HttpRequest.request().withPath("/secure/offenders/crn/$crn2/registrations")

      communityApi.`when`(request2, Times.exactly(1)).respond(
        HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(registrationResponse()),
      )
    }
  }

  fun getNotFoundRegistration(crn: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/registrations")

    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.notFoundResponse(),
    )
  }

  fun getEmptyRegistration(crn: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/registrations")

    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(emptyRegistrationResponse()),
    )
  }

  fun getNsi(crn: String, convictionId: Int, nsiCode: String? = "BRE01") {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/convictions/$convictionId/nsis")

    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(nsiResponse(nsiCode)),
    )
  }
}
