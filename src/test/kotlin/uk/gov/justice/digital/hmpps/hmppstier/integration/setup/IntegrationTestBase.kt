package uk.gov.justice.digital.hmpps.hmppstier.integration.setup

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.arnsApi.ArnsApiExtension
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.arnsApi.ArnsApiExtension.Companion.arnsApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.hmppsAuth.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import uk.gov.justice.digital.hmpps.hmppstier.messaging.consumer.DomainEvent
import uk.gov.justice.digital.hmpps.hmppstier.messaging.consumer.SQSMessage
import uk.gov.justice.digital.hmpps.hmppstier.messaging.publisher.TierCalculationDomainEvent
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.util.*

enum class TierApiVersion(private val prefix: String) {
    LEGACY(""),
    V2("/v2"),
    V3("/v3");

    fun path(path: String) = "$prefix$path"
}

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

    @MockitoSpyBean
    lateinit var tierCalculationService: TierCalculationService

    @MockitoSpyBean
    lateinit var telemetryClient: TelemetryClient

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
    internal lateinit var jwtHelper: JwtAuthorisationHelper

    @Autowired
    protected lateinit var tierCalculationRepository: TierCalculationRepository

    @BeforeEach
    fun `purge Queues`() {
        calculationCompleteClient.purgeQueue(
            PurgeQueueRequest.builder().queueUrl(calculationCompleteQueue.queueUrl).build(),
        ).get()
        domainEventQueueClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(domainEventQueue.queueUrl).build()).get()
        domainEventQueueDlqClient!!.purgeQueue(PurgeQueueRequest.builder().queueUrl(domainEventQueue.dlqUrl).build())
            .get()
        val toDelete = tierCalculationRepository.findAll().filter { it.crn !in setOf("F987546", "F987564") }
        tierCalculationRepository.deleteAllById(toDelete.mapNotNull { it.id })
    }

    fun restOfSetupWithMaleOffenderNoSevereNeeds(
        crn: String,
        assessmentId: Long,
    ) {
        arnsApi.getTierAssessmentDetails(crn, assessmentId, mapOf(), mapOf())
    }

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

    fun expectTierChangedById(tierScore: String, version: TierApiVersion = TierApiVersion.V2) {
        oneMessageCurrentlyOnQueue(calculationCompleteClient, calculationCompleteQueue.queueUrl)
        val changeEvent = tierChangeEvent()
        val crn = changeEvent.crn()
        val calculationId = changeEvent.calculationId()
        val detailUrl = "http://localhost:8080/crn/$crn/tier/$calculationId"
        assertThat(changeEvent.detailUrl).isEqualTo(detailUrl)
        assertThat(changeEvent.eventType).isEqualTo("tier.calculation.complete")
        assertThat(ZonedDateTime.parse(changeEvent.occurredAt, ISO_OFFSET_DATE_TIME)).isNotNull
        tierCalculationResult(crn, calculationId.toString(), version)
            .andExpect(status().isOk)
            .andExpect(jsonPath("tierScore", equalTo(tierScore)))
    }

    fun expectTierCalculationNotFound(crn: String, id: String, version: TierApiVersion = TierApiVersion.V2) =
        tierCalculationResult(crn, id, version).andExpect(status().isNotFound)

    fun expectTierCalculationBadRequest(crn: String, id: String, version: TierApiVersion = TierApiVersion.V2) =
        tierCalculationResult(crn, id, version).andExpect(status().isBadRequest)

    internal fun tierCalculationResult(crn: String, id: String, version: TierApiVersion = TierApiVersion.V2) =
        request(version.path("/crn/$crn/tier/$id"))

    internal fun latestTierCalculationResult(crn: String, version: TierApiVersion = TierApiVersion.V2) =
        request(version.path("/crn/$crn/tier"))

    internal fun tierHistory(crn: String, version: TierApiVersion = TierApiVersion.V2) =
        request(version.path("/crn/$crn/tier/history"))

    private fun request(uri: String) =
        mockMvc.perform(get(uri).headers(setAuthorisation()).contentType("application/json"))

    fun expectLatestTierCalculation(tierScore: String, version: TierApiVersion = TierApiVersion.V2) {
        oneMessageCurrentlyOnQueue(calculationCompleteClient, calculationCompleteQueue.queueUrl)
        val crn: String = tierChangeEvent().crn()
        latestTierCalculationResult(crn, version)
            .andExpect(status().isOk)
            .andExpect(jsonPath("tierScore", equalTo(tierScore)))
    }

    fun expectLatestTierCalculationNotFound(crn: String, version: TierApiVersion = TierApiVersion.V2) =
        latestTierCalculationResult(crn, version).andExpect(status().isNotFound)

    private fun tierChangeEvent(): TierCalculationDomainEvent {
        val message = calculationCompleteClient.receiveMessage(
            ReceiveMessageRequest.builder().queueUrl(calculationCompleteQueue.queueUrl).build(),
        ).get()
        val sqsMessage: SQSMessage = objectMapper.readValue(message.messages()[0].body(), SQSMessage::class.java)
        return objectMapper.readValue(sqsMessage.message, TierCalculationDomainEvent::class.java)
    }

    fun TierCalculationDomainEvent.crn(): String = this.personReference.identifiers[0].value

    fun TierCalculationDomainEvent.calculationId(): UUID = this.additionalInformation.calculationId

    internal fun setAuthorisation() =
        jwtHelper.setAuthorisationHeader(roles = listOf("HMPPS_TIER", "MANAGEMENT_TIER_UPDATE", "TIER_API_QUEUE_ADMIN"))

    fun sendDomainEvent(
        message: DomainEvent,
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
