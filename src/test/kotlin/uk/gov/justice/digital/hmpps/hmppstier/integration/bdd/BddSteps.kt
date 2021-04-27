package uk.gov.justice.digital.hmpps.hmppstier.integration.bdd

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import com.google.gson.Gson
import io.cucumber.java8.En
import io.cucumber.java8.Scenario
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
import org.mockserver.model.Parameter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.SQSMessage
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.communityApiAssessmentsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.custodialNCConvictionResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.getNumberOfMessagesCurrentlyOnQueue
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.maleOffenderResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.putMessageOnQueue
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithRosh
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import uk.gov.justice.digital.hmpps.hmppstier.service.TierChangeEvent
import java.math.BigDecimal

class BddSteps : En {

  @Autowired
  lateinit var gson: Gson

  @Autowired
  lateinit var calculationCompleteClient: AmazonSQSAsync

  @Value("\${calculation-complete.sqs-queue}")
  lateinit var calculationCompleteUrl: String

  @Autowired
  lateinit var offenderEventsClient: AmazonSQSAsync

  @Value("\${offender-events.sqs-queue}")
  lateinit var eventQueueUrl: String

  @Autowired
  lateinit var tierCalculationRepository: TierCalculationRepository

  lateinit var setupData: SetupData

  private var oauthMock: ClientAndServer = ClientAndServer.startClientAndServer(9090)
  private var communityApi: ClientAndServer = ClientAndServer.startClientAndServer(8091)
  private var assessmentApi: ClientAndServer = ClientAndServer.startClientAndServer(8092)

  private fun setupOauth() {
    val response = HttpResponse.response().withContentType(MediaType.APPLICATION_JSON)
      .withBody(gson.toJson(mapOf("access_token" to "ABCDE", "token_type" to "bearer")))
    oauthMock.`when`(HttpRequest.request().withPath("/auth/oauth/token").withBody("grant_type=client_credentials")).respond(response)
  }

  init {

    Before { scenario: Scenario ->
      offenderEventsClient.purgeQueue(PurgeQueueRequest(eventQueueUrl))
      calculationCompleteClient.purgeQueue(PurgeQueueRequest(calculationCompleteUrl))

      setupOauth()
      setupData = SetupData(communityApi)
      tierCalculationRepository.deleteAll()
    }

    After { scenario: Scenario ->
      communityApi.stop()
      assessmentApi.stop()
      oauthMock.stop()
    }
    Given("an RSR score of {string}") { rsr: String ->
      setupData.setRsr(rsr)
    }
    Given("a ROSH score of {string}") { rosh: String ->
      setupData.setRosh(Rosh.valueOf(rosh).registerCode)
    }
    And("no ROSH score") {
      // Do nothing
    }
    And("no RSR score"){
      setupData.setRsr("0")
    }
    When("a tier is calculated") {
      setupData.doSetup()
      putMessageOnQueue(offenderEventsClient, eventQueueUrl, "X12345")
    }

    Then("{string} points are scored") { points: String ->
      await untilCallTo {
        getNumberOfMessagesCurrentlyOnQueue(
          calculationCompleteClient,
          calculationCompleteUrl
        )
      } matches { it == 1 }
      val message = calculationCompleteClient.receiveMessage(calculationCompleteUrl)
      val sqsMessage: SQSMessage = gson.fromJson(message.messages[0].body, SQSMessage::class.java)
      val changeEvent: TierChangeEvent = gson.fromJson(sqsMessage.Message, TierChangeEvent::class.java)

      val calculation: TierCalculationEntity? = tierCalculationRepository.findByCrnAndUuid("X12345", changeEvent.calculationId)
      assertThat(calculation?.data?.protect?.points).isEqualTo(Integer.valueOf(points))
      // also check reason?
    }
  }

  class SetupData constructor (val communityApi: ClientAndServer) {
    private var rosh: String = "NO_ROSH"
    private var rsr: BigDecimal = BigDecimal(0)

    fun setRsr(rsr: String) {
      this.rsr = BigDecimal(rsr)
    }
    
    fun setRosh(rosh: String){
      this.rosh = rosh
    }

    fun doSetup() {
      // RSR BDD
      communityApiResponse(communityApiAssessmentsResponse(rsr), "/secure/offenders/crn/X12345/assessments")
      // ROSH BDD
      communityApiResponse(registrationsResponseWithRosh(rosh), "/secure/offenders/crn/X12345/registrations")
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

    fun communityApiResponse(response: HttpResponse, urlTemplate: String) =
      httpSetup(response, urlTemplate, communityApi)
  }
}
