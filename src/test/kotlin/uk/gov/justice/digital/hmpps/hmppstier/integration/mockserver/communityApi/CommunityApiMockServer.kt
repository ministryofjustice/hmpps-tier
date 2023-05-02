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
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.convictionsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.domain.Registration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.domain.Sentence
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

  fun getNoSentenceConviction(crn: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/convictions").withQueryStringParameter("activeOnly", "true")
    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(convictionsResponse(Conviction(sentence = null)))
    )
  }
  fun getCustodialNCSentenceConviction(crn: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/convictions").withQueryStringParameter("activeOnly", "true")

    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(convictionsResponse(Conviction()))
    )
  }

  fun getCustodialSCSentenceConviction(crn: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/convictions").withQueryStringParameter("activeOnly", "true")

    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(convictionsResponse(Conviction(sentence = Sentence(sentenceCode = "SC"))))
    )
  }

  fun getCommunitySentenceConviction(crn: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/convictions").withQueryStringParameter("activeOnly", "true")
    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(convictionsResponse(Conviction(sentence = Sentence(sentenceCode = "SP"))))
    )
  }

  fun getOneActiveAndOneInactiveCommunityConviction(crn: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/convictions").withQueryStringParameter("activeOnly", "true")
    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(
        convictionsResponse(
          Conviction(id = 2500409583, sentence = Sentence(sentenceCode = "SP")),
          Conviction(id = 2500409584, active = false, sentence = Sentence(sentenceCode = "SP"))
        )
      )
    )
  }

  fun getOneActiveCustodialAndOneActiveCommunityConviction(crn: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/convictions").withQueryStringParameter("activeOnly", "true")
    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(
        convictionsResponse(
          Conviction(id = 2500409583),
          Conviction(id = 2500409584, convictionDate = LocalDate.of(2021,1,12), sentence = Sentence(sentenceCode = "SP"))
        )
      )
    )
  }

  fun getOneInactiveCustodialConviction(crn: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/convictions").withQueryStringParameter("activeOnly", "true")
    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(convictionsResponse(Conviction(id = 2500409583, active = false, sentence = Sentence(terminationDate = LocalDate.now().minusDays(1)))))
    )
  }

  fun getOneInactiveCommunityConviction(crn: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/convictions").withQueryStringParameter("activeOnly", "true")
    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(convictionsResponse(Conviction(id = 2500409583, active = false, sentence = Sentence(sentenceCode = "SP", terminationDate = LocalDate.now().minusDays(1)))))
    )
  }

  fun getMappaRegistration(crn: String, level: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/registrations").withQueryStringParameter("activeOnly", "true")

    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(registrationResponse(Registration(registerLevel = level)))
    )
  }

  fun getEmptyRegistration(crn: String) {
    val request = HttpRequest.request().withPath("/secure/offenders/crn/$crn/registrations").withQueryStringParameter("activeOnly", "true")
    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(registrationResponse())
    )
  }




}
