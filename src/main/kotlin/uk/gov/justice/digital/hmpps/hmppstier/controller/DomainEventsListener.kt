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
                calculateTier(message.crn, message.eventType, message.changeReason())
                message.sourceCrn?.also {
                    calculator.deleteCalculationsForCrn(it, message.eventType)
                }
            }

            "probation-case.unmerge.completed" -> {
                calculateTier(message.unmergedCrn, message.eventType, message.changeReason())
                calculateTier(message.reactivatedCrn, message.eventType, message.changeReason())
            }

            else -> calculateTier(message.crn, message.eventType, message.changeReason(), message.recalculationSource)
        }
    }

    private fun calculateTier(
        crn: String?,
        eventType: String,
        changeReason: String,
        recalculationSource: String? = null
    ) = crn?.also {
        val source = recalculationSource?.let { RecalculationSource.of(recalculationSource, eventType, changeReason) }
            ?: RecalculationSource.EventSource.DomainEventRecalculation(eventType, changeReason)
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
    val description: String,
    val personReference: PersonReference,
    val additionalInformation: Map<String, Any>? = mapOf(),
) {
    val crn = personReference.identifiers.firstOrNull { it.type == "CRN" }?.value
    val sourceCrn = additionalInformation?.get("sourceCRN") as String?
    val targetCrn = additionalInformation?.get("targetCRN") as String?
    val unmergedCrn = additionalInformation?.get("unmergedCRN") as String?
    val reactivatedCrn = additionalInformation?.get("reactivatedCRN") as String?
    val recalculationSource = additionalInformation?.get("recalculationSource") as String?

    fun changeReason(): String =
        when (eventType) {
            "enforcement.breach.concluded" -> "A breach was concluded"
            "enforcement.breach.raised" -> "A breach was raised"
            "enforcement.recall.concluded" -> "A recall to custody process was concluded"
            "enforcement.recall.raised" -> "A recall to custody process was started"
            "probation-case.engagement.created" -> "The case was created"
            "probation-case.merge.completed" -> "The case was merged from $sourceCrn into $targetCrn"
            "probation-case.unmerge.completed" -> "The case was un-merged from $unmergedCrn and $reactivatedCrn"
            "probation-case.registration.added" -> "${registrationOfType()} was added"
            "probation-case.registration.deleted" -> "${registrationOfType()} was removed"
            "probation-case.registration.deregistered" -> "${registrationOfType()} was removed"
            "probation-case.registration.updated" -> "${registrationOfType()} was updated"
            "probation-case.requirement.created" -> "${requirementOfType()} was added"
            "probation-case.requirement.deleted" -> "${requirementOfType()} was removed"
            "probation-case.requirement.terminated" -> "${requirementOfType()} was terminated"
            "probation-case.requirement.unterminated" -> "${requirementOfType()} was un-terminated"
            "risk-assessment.scores.determined" -> "An OASys assessment was produced"

            else -> description
        }

    private fun registrationOfType() = "A registration" +
        (additionalInformation?.get("registerTypeDescription")?.let { " of type '$it'" } ?: "")

    private fun requirementOfType() = "A requirement" +
        (additionalInformation?.get("requirementMainType")?.let { " of type '$it'" } ?: "")
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
