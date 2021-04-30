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
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
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
import java.lang.IllegalArgumentException
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
      setupData = SetupData(communityApi, assessmentApi)
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

    Given("an offender scores 21 points") {
      setupData.setOgrs("90") // 9 points
      setupData.setNeeds(
        mapOf(
          "ACCOMMODATION" to "STANDARD_NEED",
          "EDUCATION_TRAINING_AND_EMPLOYABILITY" to "STANDARD_NEED",
          "RELATIONSHIPS" to "STANDARD_NEED",
          "LIFESTYLE_AND_ASSOCIATES" to "STANDARD_NEED",
          "DRUG_MISUSE" to "STANDARD_NEED",
          "ALCOHOL_MISUSE" to "STANDARD_NEED"
        )
      ) // 12 points
    }

    Given("an offender scores 20 points") {
      setupData.setOgrs("100") // 10 points
      setupData.setNeeds(
        mapOf(
          "ACCOMMODATION" to "STANDARD_NEED",
          "EDUCATION_TRAINING_AND_EMPLOYABILITY" to "STANDARD_NEED",
          "RELATIONSHIPS" to "STANDARD_NEED",
          "LIFESTYLE_AND_ASSOCIATES" to "STANDARD_NEED",
          "DRUG_MISUSE" to "STANDARD_NEED"
        )
      ) // 10 points
    }

    Given("an offender scores 19 points") {
      setupData.setOgrs("90") // 9 points
      setupData.setNeeds(
        mapOf(
          "ACCOMMODATION" to "STANDARD_NEED",
          "EDUCATION_TRAINING_AND_EMPLOYABILITY" to "STANDARD_NEED",
          "RELATIONSHIPS" to "STANDARD_NEED",
          "LIFESTYLE_AND_ASSOCIATES" to "STANDARD_NEED",
          "DRUG_MISUSE" to "STANDARD_NEED"
        )
      ) // 10 points
    }

    Given("an offender scores 11 points") {
      setupData.setOgrs("90") // 9 points
      setupData.setNeeds(
        mapOf(
          "ACCOMMODATION" to "STANDARD_NEED",
        )
      ) // 2 points
    }

    Given("an offender scores 10 points") {
      setupData.setOgrs("100") // 10 points
    }

    Given("an offender scores 9 points") {
      setupData.setOgrs("90") // 9 points
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

      val calculation: TierCalculationEntity? = tierCalculationRepository.findByCrnAndUuid("X12345", changeEvent.calculationId)
      assertThat(calculation?.data?.protect?.points).isEqualTo(Integer.valueOf(points))
      // also check reason?
    }

    Then("a Change level of {string} is returned") { changeLevel: String ->
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
      assertThat(calculation?.data?.change?.tier?.value).isEqualTo(Integer.valueOf(changeLevel))
      // also check reason?
    }
  }
}
