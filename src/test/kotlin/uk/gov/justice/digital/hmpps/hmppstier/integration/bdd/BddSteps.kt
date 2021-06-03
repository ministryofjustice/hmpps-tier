package uk.gov.justice.digital.hmpps.hmppstier.integration.bdd

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import com.google.gson.Gson
import io.cucumber.java8.En
import io.cucumber.java8.Scenario
import org.assertj.core.api.Assertions.assertThat
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType.APPLICATION_JSON
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa.M1
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa.M3
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh.HIGH
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh.MEDIUM
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.SQSMessage
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.oneMessageCurrentlyOnQueue
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.putMessageOnQueue
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import uk.gov.justice.digital.hmpps.hmppstier.service.TierChangeEvent
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

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

  @Autowired
  lateinit var oauthMock: ClientAndServer

  @Autowired
  private lateinit var communityApi: ClientAndServer

  @Autowired
  private lateinit var assessmentApi: ClientAndServer

  private lateinit var setupData: SetupData
  private lateinit var crn: String
  private lateinit var assessmentId: String

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
      crn = UUID.randomUUID().toString().replace("-", "").substring(0, 7)
      assessmentId = UUID.randomUUID().toString().replace("\\D+".toRegex(), "").padEnd(11, '1').substring(0, 11)
      setupData = SetupData(communityApi, assessmentApi, mapOf("crn" to crn, "assessmentId" to "1$assessmentId"))
    }

    Given("an RSR score of {string}") { rsr: String ->
      setupData.setRsr(rsr)
    }
    Given("a ROSH score of {string}") { rosh: String ->
      val roshCode: String =
        if (Rosh.values().any { it.name == rosh }) Rosh.valueOf(rosh).registerCode
        else "NO_ROSH"

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

    Given("an OGRS score of {string}%") { ogrs: String ->
      setupData.setOgrs(ogrs)
    }

    Given("the assessment need {string} with severity {string}") { need: String, severity: String ->
      setupData.addNeed(need, severity)
    }

    Given("an offender scores 21 change points") {
      setupData.setOgrs("90") // 9 points
      setupData.setNeeds(
        mapOf(
          "ACCOMMODATION" to "SEVERE",
          "EDUCATION_TRAINING_AND_EMPLOYABILITY" to "SEVERE",
          "RELATIONSHIPS" to "SEVERE",
          "LIFESTYLE_AND_ASSOCIATES" to "SEVERE",
          "DRUG_MISUSE" to "SEVERE",
          "ALCOHOL_MISUSE" to "SEVERE"
        )
      ) // 12 points
    }

    Given("an offender scores 20 change points") {
      setupData.setOgrs("100") // 10 points
      setupData.setNeeds(
        mapOf(
          "ACCOMMODATION" to "SEVERE",
          "EDUCATION_TRAINING_AND_EMPLOYABILITY" to "SEVERE",
          "RELATIONSHIPS" to "SEVERE",
          "LIFESTYLE_AND_ASSOCIATES" to "SEVERE",
          "DRUG_MISUSE" to "SEVERE"
        )
      ) // 10 points
    }

    Given("an offender scores 19 change points") {
      setupData.setOgrs("90") // 9 points
      setupData.setNeeds(
        mapOf(
          "ACCOMMODATION" to "SEVERE",
          "EDUCATION_TRAINING_AND_EMPLOYABILITY" to "SEVERE",
          "RELATIONSHIPS" to "SEVERE",
          "LIFESTYLE_AND_ASSOCIATES" to "SEVERE",
          "DRUG_MISUSE" to "SEVERE"
        )
      ) // 10 points
    }

    Given("an offender scores 11 change points") {
      setupData.setOgrs("90") // 9 points
      setupData.setNeeds(
        mapOf(
          "ACCOMMODATION" to "SEVERE",
        )
      ) // 2 points
    }

    Given("an offender scores 10 change points") {
      setupData.setNeeds(
        mapOf(
          "ACCOMMODATION" to "SEVERE",
          "EDUCATION_TRAINING_AND_EMPLOYABILITY" to "SEVERE",
          "RELATIONSHIPS" to "SEVERE",
          "LIFESTYLE_AND_ASSOCIATES" to "SEVERE",
          "DRUG_MISUSE" to "SEVERE"
        )
      ) // 10 points
    }

    Given("an offender scores 9 change points") {
      setupData.setNeeds(
        mapOf(
          "ACCOMMODATION" to "SEVERE",
          "EDUCATION_TRAINING_AND_EMPLOYABILITY" to "SEVERE",
          "RELATIONSHIPS" to "SEVERE",
          "LIFESTYLE_AND_ASSOCIATES" to "SEVERE",
          "DRUG_MISUSE" to "STANDARD"
        )
      ) // 9 points
    }

    Given("an offender scores 31 protect points") {
      setupData.setMappa(M1.registerCode) // 5
      setupData.setRosh(HIGH.registerCode) // 20
      setupData.setAdditionalFactors(listOf("RCCO", "RCPR", "RCHD")) // 6
    }
    Given("an offender scores 152 protect points") {
      setupData.setMappa(M3.registerCode) // 150
      setupData.setAdditionalFactors(listOf("RCCO")) // 2
    }
    Given("an offender scores 150 protect points") {
      setupData.setMappa(M3.registerCode)
    }
    Given("an offender scores 51 protect points") {
      setupData.setGender("Female")
      setupData.setAssessmentAnswer("11.2", "1") // 2
      setupData.setAssessmentAnswer("6.9", "YES") // 2
      setupData.setMappa(M1.registerCode) // 5
      setupData.setRosh(HIGH.registerCode) // 20
      setupData.setNsiOutcomes(listOf("BRE02"))
      setupData.setAdditionalFactors(
        listOf(
          "RMDO",
          "ALSH",
          "RVLN",
          "RCCO",
          "RCPR",
          "RCHD",
          "RPIR",
          "RVAD",
          "STRG",
          "RTAO"
        )
      ) // 20
    }
    Given("an offender scores 21 protect points") {
      setupData.setMappa(M1.registerCode) // 5
      setupData.setRosh(MEDIUM.registerCode) // 10
      setupData.setAdditionalFactors(listOf("RVAD", "STRG", "RMDO")) // 6
    }
    Given("an offender scores 20 protect points") {
      setupData.setRosh(HIGH.registerCode)
    }
    Given("an offender scores 19 protect points") {
      setupData.setMappa(M1.registerCode) // 5
      setupData.setRosh(MEDIUM.registerCode) // 10
      setupData.setAdditionalFactors(listOf("ALSH", "RVLN")) // 4
    }
    Given("an offender scores 11 protect points") {
      setupData.setMappa(M1.registerCode) // 5
      setupData.setAdditionalFactors(listOf("RVAD", "ALSH", "RVLN")) // 6
    }
    Given("an offender scores 10 protect points") {
      setupData.setRosh(MEDIUM.registerCode) // 10
    }
    Given("an offender scores 9 protect points") {
      setupData.setMappa(M1.registerCode) // 5
      setupData.setAdditionalFactors(listOf("ALSH", "RVLN")) // 4
    }
    Given("an offender scores 0 protect points") {
      // do nothing
    }
    Given("an offender with a current sentence of type {string}") { sentenceType: String ->
      setupData.setSentenceType(sentenceType)
    }
    Given("an offender with a current non-custodial sentence") {
      setupData.setSentenceType("SP")
    }
    And("unpaid work") {
      setupData.setUnpaidWork()
    }
    And("order extended") {
      setupData.setOrderExtended()
    }
    And("a non restrictive requirement") {
      setupData.setNonRestrictiveRequirement()
    }
    And("a valid assessment") {
      setupData.setValidAssessment()
    }
    And("no completed Layer 3 assessment") {
      // do nothing - maybe should be a 404 from assessments API?
    }
    And("a completed Layer 3 assessment dated 55 weeks and one day ago") {
      setupData.setAssessmentDate(LocalDateTime.now().minusWeeks(55).minusDays(1))
    }
    And("a completed Layer 3 assessment dated 55 weeks ago") {
      setupData.setAssessmentDate(LocalDateTime.now().minusWeeks(55))
    }
    And("has the following OASys complexity answer: {string} {string} : {string}") { _: String, question: String, answer: String ->
      setupData.setAssessmentAnswer(question, answer)
    }
    And("has an active conviction with NSI Outcome code {string}") { outcome: String ->
      setupData.setNsiOutcomes(listOf(outcome))
    }
    And("has two active convictions with NSI Outcome codes {string} and {string}") { outcome1: String, outcome2: String ->
      setupData.setTwoActiveConvictions()
      setupData.setNsiOutcomes(listOf(outcome1, outcome2))
    }
    And("has two active convictions with NSI Outcome code {string}") { outcome: String ->
      setupData.setNsiOutcomes(listOf(outcome))
      setupData.setTwoActiveConvictions()
    }
    And("has a conviction terminated 365 days ago with NSI Outcome code {string}") { outcome: String ->
      setupData.setConvictionTerminatedDate(LocalDate.now().minusYears(1))
      setupData.setNsiOutcomes(listOf(outcome))
    }
    And("has a conviction terminated 366 days ago with NSI Outcome code {string}") { outcome: String ->
      setupData.setConvictionTerminatedDate(LocalDate.now().minusYears(1).minusDays(1))
      setupData.setNsiOutcomes(listOf(outcome))
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
      putMessageOnQueue(offenderEventsClient, eventQueueUrl, crn)
    }

    Then("{int} protect points are scored") { points: Int ->
      val calculation: TierCalculationEntity = getTier()
      assertThat(calculation.data.protect.points).isEqualTo(points)
    }

    Then("{int} change points are scored") { points: Int ->
      val calculation: TierCalculationEntity = getTier()
      assertThat(calculation.data.change.points).isEqualTo(points)
    }

    Then("there is a mandate for change") {
      val calculation: TierCalculationEntity = getTier()
      assertThat(calculation.data.change.tier.value).isEqualTo(1)
    }

    Then("a change level of {int} is returned and {int} points are scored") { changeLevel: Int, points: Int ->
      changeIs(changeLevel, points)
    }

    Then("a protect level of {string} is returned and {int} points are scored") { protectLevel: String, points: Int ->
      val calculation: TierCalculationEntity = getTier()
      assertThat(calculation.data.protect.tier).isEqualTo(ProtectLevel.valueOf(protectLevel))
      assertThat(calculation.data.protect.points).isEqualTo(points)
    }

    Then("there is no mandate for change") {
      changeIs(0, 0)
    }
  }

  private fun changeIs(tier: Int, points: Int) {
    val calculation: TierCalculationEntity = getTier()
    assertThat(calculation.data.change.tier.value).isEqualTo(tier)
    assertThat(calculation.data.change.points).isEqualTo(points)
  }

  private fun getTier(): TierCalculationEntity {
    oneMessageCurrentlyOnQueue(calculationCompleteClient, calculationCompleteUrl)
    val message = calculationCompleteClient.receiveMessage(calculationCompleteUrl)
    val sqsMessage: SQSMessage = gson.fromJson(message.messages[0].body, SQSMessage::class.java)
    val changeEvent: TierChangeEvent = gson.fromJson(sqsMessage.Message, TierChangeEvent::class.java)

    return tierCalculationRepository.findByCrnAndUuid(crn, changeEvent.calculationId)!!
  }
}
