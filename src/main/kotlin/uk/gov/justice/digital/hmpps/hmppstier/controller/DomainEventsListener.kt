package uk.gov.justice.digital.hmpps.hmppstier.controller

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.dao.CannotAcquireLockException
import org.springframework.jdbc.CannotGetJdbcConnectionException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.CannotCreateTransactionException
import org.springframework.transaction.UnexpectedRollbackException
import org.springframework.web.client.RestClientException
import uk.gov.justice.digital.hmpps.hmppstier.config.retry
import uk.gov.justice.digital.hmpps.hmppstier.service.RecalculationSource
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService

@Service
@ConditionalOnProperty("messaging.consumer.enabled", matchIfMissing = true)
class DomainEventsListener(
    private val calculator: TierCalculationService,
    private val objectMapper: ObjectMapper,
) {

    @SqsListener("hmppsdomaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
    fun listen(msg: String) {
        val (message, attributes) = objectMapper.readValue<SQSMessage>(msg)
        val domainEventMessage = objectMapper.readValue<DomainEventsMessage>(message)
        if (attributes.eventType == "risk-assessment.scores.determined" && domainEventMessage.eventType != "assessment.summary.produced") {
            return
        }
        retry(3, RETRYABLE_EXCEPTIONS) { handleMessage(domainEventMessage) }
    }

    private fun handleMessage(message: DomainEventsMessage) {
        when (message.eventType) {
            "probation-case.deleted.gdpr" -> message.crn?.also {
                calculator.deleteCalculationsForCrn(
                    it,
                    message.eventType
                )
            }

            "probation-case.merge.completed" -> {
                calculateTier(message.crn, message.eventType)
                message.sourceCrn?.also {
                    calculator.deleteCalculationsForCrn(it, message.eventType)
                }
            }

            "probation-case.unmerge.completed" -> {
                calculateTier(message.unmergedCrn, message.eventType)
                calculateTier(message.reactivatedCrn, message.eventType)
            }

            else -> calculateTier(message.crn, message.eventType, message.dryRun, message.recalculationSource)
        }
    }

    private fun calculateTier(
        crn: String?,
        eventType: String,
        dryRun: Boolean = false,
        recalculationSource: String? = null
    ) = crn?.also {
        val source = recalculationSource?.let { RecalculationSource.of(recalculationSource, eventType) }
            ?: RecalculationSource.EventSource.DomainEventRecalculation(eventType)
        calculator.calculateTierForCrn(crn, source, true)
    }

    companion object {
        val RETRYABLE_EXCEPTIONS = listOf(
            RestClientException::class,
            CannotAcquireLockException::class,
            ObjectOptimisticLockingFailureException::class,
            CannotCreateTransactionException::class,
            CannotGetJdbcConnectionException::class,
            UnexpectedRollbackException::class
        )
    }
}

data class DomainEventsMessage(
    val eventType: String,
    val personReference: PersonReference,
    val additionalInformation: Map<String, Any>? = mapOf(),
) {
    val crn = personReference.identifiers.firstOrNull { it.type == "CRN" }?.value
    val sourceCrn = additionalInformation?.get("sourceCRN") as String?
    val unmergedCrn = additionalInformation?.get("unmergedCRN") as String?
    val reactivatedCrn = additionalInformation?.get("reactivatedCRN") as String?
    val dryRun = additionalInformation?.get("dryRun") == true
    val recalculationSource = additionalInformation?.get("recalculationSource") as String?
}

data class PersonReference(
    val identifiers: List<Identifiers>,
)

data class Identifiers(
    val type: String,
    val value: String,
)

data class SQSMessage(
    @JsonProperty("Message") val message: String,
    @JsonProperty("MessageAttributes") val attributes: MessageAttributes = MessageAttributes(),
)

data class MessageAttributes(
    @JsonAnyGetter @JsonAnySetter
    private val attributes: MutableMap<String, MessageAttribute> = mutableMapOf(),
) : MutableMap<String, MessageAttribute> by attributes {

    val eventType = attributes[EVENT_TYPE_KEY]?.value

    companion object {
        private const val EVENT_TYPE_KEY = "eventType"
    }
}

data class MessageAttribute(@JsonProperty("Type") val type: String, @JsonProperty("Value") val value: String)
