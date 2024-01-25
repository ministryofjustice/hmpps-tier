package uk.gov.justice.digital.hmpps.hmppstier.integration.setup

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppstier.controller.DomainEventsMessage
import uk.gov.justice.digital.hmpps.hmppstier.controller.SQSMessage
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.arnsApi.ArnsApiExtension
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.arnsApi.ArnsApiExtension.Companion.arnsApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.hmppsAuth.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService
import uk.gov.justice.digital.hmpps.hmppstier.service.TierChangeEvent
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.util.*

@ExtendWith(
    ArnsApiExtension::class,
    HmppsAuthApiExtension::class,
    TierToDeliusApiExtension::class,
)
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var hmppsQueueService: HmppsQueueService

    @SpyBean
    lateinit var tierCalculationService: TierCalculationService

    @SpyBean
    lateinit var telemetryClient: TelemetryClient

    private val offenderEventsQueue by lazy {
        hmppsQueueService.findByQueueId("hmppsoffenderqueue")
            ?: throw MissingQueueException("HmppsQueue hmppsoffenderqueue not found")
    }
    private val offenderEventsDlqClient by lazy { offenderEventsQueue.sqsDlqClient }
    private val offenderEventsClient by lazy { offenderEventsQueue.sqsClient }

    private val domainEventQueue by lazy {
        hmppsQueueService.findByQueueId("hmppsdomaineventsqueue")
            ?: throw MissingQueueException("HmppsQueue hmppsdomaineventsqueue not found")
    }
    private val domainEventQueueDlqClient by lazy { domainEventQueue.sqsDlqClient }
    private val domainEventQueueClient by lazy { domainEventQueue.sqsClient }

    private val calculationCompleteQueue by lazy {
        hmppsQueueService.findByQueueId("hmppscalculationcompletequeue")
            ?: throw MissingQueueException("HmppsQueue hmppscalculationcompletequeue not found")
    }
    private val calculationCompleteClient by lazy { calculationCompleteQueue.sqsClient }

    @Autowired
    internal lateinit var jwtHelper: JwtAuthHelper

    @Autowired
    private lateinit var tierCalculationRepository: TierCalculationRepository

    @BeforeEach
    fun `purge Queues`() {
        offenderEventsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(offenderEventsQueue.queueUrl).build())
            .get()
        calculationCompleteClient.purgeQueue(
            PurgeQueueRequest.builder().queueUrl(calculationCompleteQueue.queueUrl).build(),
        ).get()
        offenderEventsDlqClient!!.purgeQueue(PurgeQueueRequest.builder().queueUrl(offenderEventsQueue.dlqUrl).build())
            .get()
        domainEventQueueClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(domainEventQueue.queueUrl).build()).get()
        domainEventQueueDlqClient!!.purgeQueue(PurgeQueueRequest.builder().queueUrl(domainEventQueue.dlqUrl).build())
            .get()
        tierCalculationRepository.deleteAll()
    }

    fun restOfSetupWithMaleOffenderNoSevereNeeds(
        crn: String,
        assessmentId: Long,
    ) {
        arnsApi.getTierAssessmentDetails(crn, assessmentId, mapOf(), mapOf())
    }

    fun calculateTierFor(crn: String) = putMessageOnQueue(offenderEventsClient, offenderEventsQueue.queueUrl, crn)
    fun calculateTierForDomainEvent(crn: String) = putMessageOnDomainQueue(
        domainEventQueueClient,
        domainEventQueue.queueUrl,
        crn,
    )

    fun calculateTierForRecallDomainEvent(crn: String) = putRecallMessageOnDomainQueue(
        domainEventQueueClient,
        domainEventQueue.queueUrl,
        crn,
    )

    fun expectTierChangedById(tierScore: String) {
        oneMessageCurrentlyOnQueue(calculationCompleteClient, calculationCompleteQueue.queueUrl)
        val changeEvent: TierChangeEvent = tierChangeEvent()
        val crn = changeEvent.crn()
        val calculationId = changeEvent.calculationId()
        val detailUrl = "http://localhost:8080/crn/$crn/tier/$calculationId"
        assertThat(changeEvent.detailUrl).isEqualTo(detailUrl)
        assertThat(changeEvent.eventType).isEqualTo("tier.calculation.complete")
        assertThat(ZonedDateTime.parse(changeEvent.occurredAt, ISO_OFFSET_DATE_TIME)).isNotNull
        tierCalculationResult(crn, calculationId.toString())
            .andExpect(status().isOk)
            .andExpect(jsonPath("tierScore", equalTo(tierScore)))
    }

    fun expectTierCalculationNotFound(crn: String, calculationId: String) =
        tierCalculationResult(crn, calculationId).andExpect(status().isNotFound)

    fun expectTierCalculationBadRequest(crn: String, calculationId: String) =
        tierCalculationResult(crn, calculationId).andExpect(status().isBadRequest)

    private fun tierCalculationResult(crn: String, calculationId: String) = request("/crn/$crn/tier/$calculationId")

    private fun request(uri: String) = mockMvc.perform(get(uri).headers(authHeaders()).contentType("application/json"))

    private fun latestTierCalculationResult(crn: String) = request("/crn/$crn/tier")

    fun expectLatestTierCalculation(tierScore: String) {
        oneMessageCurrentlyOnQueue(calculationCompleteClient, calculationCompleteQueue.queueUrl)
        val crn: String = tierChangeEvent().crn()
        latestTierCalculationResult(crn)
            .andExpect(status().isOk)
            .andExpect(jsonPath("tierScore", equalTo(tierScore)))
    }

    fun expectLatestTierCalculationNotFound(crn: String) =
        latestTierCalculationResult(crn).andExpect(status().isNotFound)

    private fun tierChangeEvent(): TierChangeEvent {
        val message = calculationCompleteClient.receiveMessage(
            ReceiveMessageRequest.builder().queueUrl(calculationCompleteQueue.queueUrl).build(),
        ).get()
        val sqsMessage: SQSMessage = objectMapper.readValue(message.messages()[0].body(), SQSMessage::class.java)
        return objectMapper.readValue(sqsMessage.message, TierChangeEvent::class.java)
    }

    fun TierChangeEvent.crn(): String = this.personReference.identifiers[0].value

    fun TierChangeEvent.calculationId(): UUID = this.additionalInformation.calculationId

    internal fun authHeaders(): HttpHeaders = HttpHeaders().apply { setBearerAuth(jwtHelper.createJwt()) }

    fun sendDomainEvent(
        message: DomainEventsMessage,
        queueUrl: String = domainEventQueue.queueUrl,
        om: ObjectMapper = objectMapper,
    ) = domainEventQueueClient.sendMessage(
        SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(
                om.writeValueAsString(SQSMessage(om.writeValueAsString(message))),
            ).build(),
    ).get()
}
