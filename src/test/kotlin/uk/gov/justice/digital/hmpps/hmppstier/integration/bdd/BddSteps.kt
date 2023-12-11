package uk.gov.justice.digital.hmpps.hmppstier.integration.bdd

import io.cucumber.java8.En
import io.cucumber.java8.Scenario
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa.M1
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa.M3
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh.HIGH
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh.MEDIUM
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.response.domain.Need
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Registration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Requirement
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.putMessageOnQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID

class BddSteps : En {

    @Qualifier("hmppscalculationcompletequeue-sqs-client")
    @Autowired
    lateinit var calculationCompleteClient: SqsAsyncClient

    private val calculationCompleteUrl by lazy {
        hmppsQueueService.findByQueueId("hmppscalculationcompletequeue")?.queueUrl
            ?: throw MissingQueueException("HmppsQueue tiercalculationqueue not found")
    }

    @Autowired
    private lateinit var hmppsQueueService: HmppsQueueService

    @Qualifier("hmppsoffenderqueue-sqs-client")
    @Autowired
    lateinit var offenderEventsClient: SqsAsyncClient

    private val eventQueueUrl by lazy {
        hmppsQueueService.findByQueueId("hmppsoffenderqueue")?.queueUrl
            ?: throw MissingQueueException("HmppsQueue tiercalculationqueue not found")
    }

    private lateinit var setupData: SetupData
    private lateinit var crn: String
    private lateinit var assessmentId: String
    private lateinit var convictionId: String
    private lateinit var secondConvictionId: String

    private fun String.capitalize(): String {
        return replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    init {

        Before { _: Scenario ->

            offenderEventsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(eventQueueUrl).build()).get()
            calculationCompleteClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(calculationCompleteUrl).build())
                .get()

            crn = UUID.randomUUID().toString().replace("-", "").substring(0, 7)
            assessmentId =
                "1${UUID.randomUUID().toString().replace("\\D+".toRegex(), "").padEnd(11, '1').substring(0, 11)}"
            convictionId =
                "1${UUID.randomUUID().toString().replace("\\D+".toRegex(), "").padEnd(11, '1').substring(0, 11)}"
            secondConvictionId = "1${convictionId.reversed()}"

            setupData = SetupData(
                mapOf(
                    "crn" to crn,
                    "assessmentId" to assessmentId,
                    "convictionId" to convictionId,
                    "secondConvictionId" to secondConvictionId,
                ),
            )
        }

        Given("an RSR score of {string}") { rsr: String ->
            setupData.setRsr(rsr)
        }
        Given("a ROSH score of {string}") { rosh: String ->
            val roshCode: String =
                if (Rosh.values().any { it.name == rosh }) {
                    Rosh.valueOf(rosh).registerCode
                } else {
                    "NO_ROSH"
                }
            setupData.addRegistration(Registration(typeCode = roshCode))
        }
        Given("an active MAPPA registration of M Level {string}") { mappa: String ->
            val mappaCode = Mappa.from("M$mappa", "MAPP")?.registerCode
            setupData.addRegistration(Registration(registerLevel = mappaCode!!))
        }
        Given("no active MAPPA Registration") {
            // Do nothing
        }
        Given("the following active registrations: {string} {string}") { _: String, additionalFactor: String ->
            additionalFactor.split(",").forEach { typeCode ->
                setupData.addRegistration(Registration(typeCode = typeCode))
            }
            setupData.setValidAssessment()
        }
        Given("an offender is {string}") { gender: String ->
            setupData.setGender(gender)
        }
        Given("an OGRS score of {string}%") { ogrs: String ->
            setupData.setOgrs(ogrs)
        }
        Given("the assessment need {string} with severity {string}") { need: String, severity: String ->
            setupData.setNeeds(Need(need.capitalize(), need, severity))
        }
        Given("an offender scores 21 change points") {
            setupData.setOgrs("90") // 9 points
            setupData.setNeeds(
                Need("Accomodation", "ACCOMMODATION", "SEVERE"),
                Need("Education Training and Employability", "EDUCATION_TRAINING_AND_EMPLOYABILITY", "SEVERE"),
                Need("Relationships", "RELATIONSHIPS", "SEVERE"),
                Need("Lifestyle and Associates", "LIFESTYLE_AND_ASSOCIATES", "SEVERE"),
                Need("Drug Misuse", "DRUG_MISUSE", "SEVERE"),
                Need("Alcohol Misuse", "ALCOHOL_MISUSE", "SEVERE"),
            ) // 12 points
        }
        Given("an offender scores 20 change points") {
            setupData.setOgrs("100") // 10 points
            setupData.setNeeds(
                Need("Accomodation", "ACCOMMODATION", "SEVERE"),
                Need("Education Training and Employability", "EDUCATION_TRAINING_AND_EMPLOYABILITY", "SEVERE"),
                Need("Relationships", "RELATIONSHIPS", "SEVERE"),
                Need("Lifestyle and Associates", "LIFESTYLE_AND_ASSOCIATES", "SEVERE"),
                Need("Drug Misuse", "DRUG_MISUSE", "SEVERE"),
            ) // 10 points
        }
        Given("an offender scores 19 change points") {
            setupData.setOgrs("90") // 9 points
            setupData.setNeeds(
                Need("Accomodation", "ACCOMMODATION", "SEVERE"),
                Need("Education Training and Employability", "EDUCATION_TRAINING_AND_EMPLOYABILITY", "SEVERE"),
                Need("Relationships", "RELATIONSHIPS", "SEVERE"),
                Need("Lifestyle and Associates", "LIFESTYLE_AND_ASSOCIATES", "SEVERE"),
                Need("Drug Misuse", "DRUG_MISUSE", "SEVERE"),
            ) // 10 points
        }
        Given("an offender scores 11 change points") {
            setupData.setOgrs("90") // 9 points
            setupData.setNeeds(Need("Accomodation", "ACCOMMODATION", "SEVERE")) // 2 points
        }
        Given("an offender scores 10 change points") {
            setupData.setNeeds(
                Need("Accomodation", "ACCOMMODATION", "SEVERE"),
                Need("Education Training and Employability", "EDUCATION_TRAINING_AND_EMPLOYABILITY", "SEVERE"),
                Need("Relationships", "RELATIONSHIPS", "SEVERE"),
                Need("Lifestyle and Associates", "LIFESTYLE_AND_ASSOCIATES", "SEVERE"),
                Need("Drug Misuse", "DRUG_MISUSE", "SEVERE"),
            ) // 10 points
        }
        Given("an offender scores 9 change points") {
            setupData.setNeeds(
                Need("Accomodation", "ACCOMMODATION", "SEVERE"),
                Need("Education Training and Employability", "EDUCATION_TRAINING_AND_EMPLOYABILITY", "SEVERE"),
                Need("Relationships", "RELATIONSHIPS", "SEVERE"),
                Need("Lifestyle and Associates", "LIFESTYLE_AND_ASSOCIATES", "SEVERE"),
                Need("Drug Misuse", "DRUG_MISUSE", "STANDARD"),
            ) // 9 points
        }
        Given("an offender scores 31 protect points") {
            setupData.addRegistration(Registration(registerLevel = M1.registerCode))
            setupData.addRegistration(Registration(typeCode = HIGH.registerCode))
            setupData.addRegistration(Registration(typeCode = "RCCO"))
            setupData.addRegistration(Registration(typeCode = "RCPR"))
            setupData.addRegistration(Registration(typeCode = "RCHD"))
        }
        Given("an offender scores 152 protect points") {
            setupData.addRegistration(Registration(registerLevel = M3.registerCode)) // 150
            setupData.addRegistration(Registration(typeCode = "RCCO")) // 2
        }
        Given("an offender scores 150 protect points") {
            setupData.addRegistration(Registration(registerLevel = M3.registerCode)) // 150
        }
        Given("an offender scores 51 protect points") {
            setupData.setGender("Female")
            setupData.setAssessmentAnswer("11.2", "1") // 2
            setupData.setAssessmentAnswer("6.9", "YES") // 2
            setupData.addRegistration(Registration(registerLevel = M1.registerCode)) // 5
            setupData.addRegistration(Registration(typeCode = HIGH.registerCode))
            setupData.addConviction(Conviction())
            setupData.setPreviousEnforcementActivity(true)
            setupData.addRegistration(Registration(typeCode = "RMDO"))
            setupData.addRegistration(Registration(typeCode = "ALSH"))
            setupData.addRegistration(Registration(typeCode = "RVLN"))
            setupData.addRegistration(Registration(typeCode = "RCCO"))
            setupData.addRegistration(Registration(typeCode = "RCPR"))
            setupData.addRegistration(Registration(typeCode = "RCHD"))
            setupData.addRegistration(Registration(typeCode = "RPIR"))
            setupData.addRegistration(Registration(typeCode = "RVAD"))
            setupData.addRegistration(Registration(typeCode = "STRG"))
            setupData.addRegistration(Registration(typeCode = "RTAO")) // 20
        }
        Given("an offender scores 21 protect points") {
            setupData.addRegistration(Registration(registerLevel = M1.registerCode)) // 5
            setupData.addRegistration(Registration(typeCode = MEDIUM.registerCode))
            setupData.addRegistration(Registration(typeCode = "RVAD"))
            setupData.addRegistration(Registration(typeCode = "STRG"))
            setupData.addRegistration(Registration(typeCode = "RMDO")) // 6
        }
        Given("an offender scores 20 protect points") {
            setupData.addRegistration(Registration(typeCode = HIGH.registerCode))
        }
        Given("an offender scores 19 protect points") {
            setupData.addRegistration(Registration(registerLevel = M1.registerCode)) // 5
            setupData.addRegistration(Registration(typeCode = MEDIUM.registerCode)) // 10
            setupData.addRegistration(Registration(typeCode = "ALSH"))
            setupData.addRegistration(Registration(typeCode = "RVLN")) // 4
        }
        Given("an offender scores 11 protect points") {
            setupData.addRegistration(Registration(registerLevel = M1.registerCode)) // 5
            setupData.addRegistration(Registration(typeCode = "RVAD"))
            setupData.addRegistration(Registration(typeCode = "ALSH"))
            setupData.addRegistration(Registration(typeCode = "RVLN"))
        }
        Given("an offender scores 10 protect points") {
            setupData.addRegistration(Registration(typeCode = MEDIUM.registerCode)) // 10
        }
        Given("an offender scores 9 protect points") {
            setupData.addRegistration(Registration(registerLevel = M1.registerCode)) // 5
            setupData.addRegistration(Registration(typeCode = "ALSH"))
            setupData.addRegistration(Registration(typeCode = "RVLN")) // 4
        }
        Given("an offender scores 0 protect points") {
            // do nothing
        }
        Given("an offender with a current sentence of type {string}") { sentenceType: String ->
            setupData.addConviction(Conviction(sentenceCode = sentenceType))
        }
        Given("an offender with a current non-custodial sentence") {
            setupData.addConviction(Conviction(sentenceCode = "SP"))
        }

        And("unpaid work") {
            setupData.addRequirement(Requirement(mainTypeCode = "W", false))
        }
        And("order extended") {
            setupData.addRequirement(Requirement(mainTypeCode = "W1", false))
            setupData.addRequirement(Requirement(mainTypeCode = "W", false))
        }
        And("a non restrictive requirement") {
            setupData.addRequirement(Requirement(mainTypeCode = "F", false))
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
        And("has an active conviction with a Previous Enforcement Activity") {
            setupData.addConviction(Conviction())
            setupData.setPreviousEnforcementActivity(true)
        }
        And("has two active convictions with a Previous Enforcement Activity") {
            setupData.addConviction(Conviction())
            setupData.addConviction(Conviction())
            setupData.setPreviousEnforcementActivity(true)
        }
        And("has two breached active convictions with a {string} Previous Enforcement Activity") { outcome1: String ->
            setupData.addConviction(Conviction(breached = true))
            setupData.addConviction(Conviction(breached = true))
            setupData.setPreviousEnforcementActivity(outcome1 == "true")
        }
        And("no ROSH score") {
            // Do nothing
        }
        And("no RSR score") {
            setupData.setRsr("0")
        }
        And("has a tier of {string}") { tier: String ->
            setupData.setCurrentTier("U$tier")
        }

        When("a tier is calculated") {
            setupData.prepareResponses()
            putMessageOnQueue(offenderEventsClient, eventQueueUrl, crn)
        }
    }
}
