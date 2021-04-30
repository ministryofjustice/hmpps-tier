package uk.gov.justice.digital.hmpps.hmppstier.integration.bdd

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import com.google.common.collect.Lists.newArrayList
import com.google.gson.Gson
import io.cucumber.java8.En
import io.cucumber.java8.Scenario
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType.APPLICATION_JSON
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.SQSMessage
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.getNumberOfMessagesCurrentlyOnQueue
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.putMessageOnQueue
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import uk.gov.justice.digital.hmpps.hmppstier.service.TierChangeEvent
import java.time.LocalDate

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

  private lateinit var setupData: SetupData

  private var oauthMock: ClientAndServer = startClientAndServer(9090)
  private var communityApi: ClientAndServer = startClientAndServer(8091)
  private var assessmentApi: ClientAndServer = startClientAndServer(8092)

  private fun setupOauth() {
    val response = response().withContentType(APPLICATION_JSON)
      .withBody(gson.toJson(mapOf("access_token" to "ABCDE", "token_type" to "bearer")))
    oauthMock.`when`(HttpRequest.request().withPath("/auth/oauth/token").withBody("grant_type=client_credentials"))
      .respond(response)
  }

  init {

    Before { _: Scenario ->
      offenderEventsClient.purgeQueue(PurgeQueueRequest(eventQueueUrl))
      calculationCompleteClient.purgeQueue(PurgeQueueRequest(calculationCompleteUrl))

      setupOauth()
      setupData = SetupData(communityApi, assessmentApi)
      tierCalculationRepository.deleteAll()
    }

    After { _: Scenario ->
      communityApi.stop()
      assessmentApi.stop()
      oauthMock.stop()
    }
    Given("an RSR score of {string}") { rsr: String ->
      setupData.setRsr(rsr)
    }
    Given("a ROSH score of {string}") { rosh: String ->
      var roshCode = "NO_ROSH"
      try {
        roshCode = Rosh.valueOf(rosh).registerCode
      } catch (e: IllegalArgumentException) {
      }
      setupData.setRosh(roshCode)
    }
    Given("an active MAPPA registration of M Level {string}") { mappa: String ->
      val mappaCode = Mappa.from("M$mappa")?.registerCode
      setupData.setMappa(mappaCode!!)
    }
    Given("no active MAPPA Registration") {
      // Do nothing
    }
    Given("the following active registrations: {string} {string}") { _: String, additionalFactor: String ->
      val additionalFactors: List<String> = additionalFactor.split(",")
      setupData.setAdditionalFactors(additionalFactors)
    }
    Given("an offender is {string}") { gender: String ->
      setupData.setGender(gender)
    }
    And("has the following OASys complexity answer: {string} {string} : {string}") { _: String, question: String, answer: String ->
      setupData.setValidAssessment()
      setupData.setAssessmentAnswer(question, answer)
    }
    And("has an active conviction with NSI Outcome code {string}") { outcome: String ->
      setupData.setNsiOutcomes(newArrayList(outcome))
    }
    And("has two active convictions with NSI Outcome codes {string} and {string}") { outcome1: String, outcome2: String ->
      setupData.setTwoActiveConvictions()
      setupData.setNsiOutcomes(newArrayList(outcome1, outcome2))
    }
    And("has two active convictions with NSI Outcome code {string}") { outcome: String ->
      setupData.setNsiOutcomes(newArrayList(outcome))
      setupData.setTwoActiveConvictions()
    }
    And("has a conviction terminated 365 days ago with NSI Outcome code {string}") { outcome: String ->
      setupData.setConvictionTerminatedDate(LocalDate.now().minusYears(1))
      setupData.setNsiOutcomes(newArrayList(outcome))
    }
    And("has a conviction terminated 366 days ago with NSI Outcome code {string}") { outcome: String ->
      setupData.setConvictionTerminatedDate(LocalDate.now().minusYears(1).minusDays(1))
      setupData.setNsiOutcomes(newArrayList(outcome))
    }
    And("no ROSH score") {
      // Do nothing
    }
    And("no RSR score") {
      setupData.setRsr("0")
    }
    And("has a custodial sentence") {
      // Do nothing
    }
    And("has a main offence of Arson") {
      setupData.setMainOffenceArson()
    }
    And("has a main offence of Violence") {
      setupData.setMainOffenceViolence()
    }
    And("has a main offence of Abstracting Electricity") {
      setupData.setMainOffenceAbstractingElectricity()
    }
    And("has a sentence length of {long} months") { months: Long ->
      setupData.setSentenceLength(months)
    }
    And("has an indeterminate sentence length") {
      setupData.setSentenceLengthIndeterminate()
    }

    When("a tier is calculated") {
      setupData.prepareResponses()
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

      val calculation: TierCalculationEntity? =
        tierCalculationRepository.findByCrnAndUuid("X12345", changeEvent.calculationId)
      assertThat(calculation?.data?.protect?.points).isEqualTo(Integer.valueOf(points))
      // also check reason?
    }
  }
}
