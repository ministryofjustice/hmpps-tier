package uk.gov.justice.digital.hmpps.hmppstier.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.service.RecalculationSource
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService

@Service
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
        handleMessage(domainEventMessage)
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
                calculateTier(message.crn)
                message.sourceCrn?.also {
                    calculator.deleteCalculationsForCrn(it, message.eventType)
                }
            }

            else -> calculateTier(message.crn)
        }
    }

    private fun calculateTier(crn: String?) = crn?.also {
        calculator.calculateTierForCrn(it, RecalculationSource.DomainEventRecalculation, true)
    }
}

data class DomainEventsMessage(
    val eventType: String,
    val personReference: PersonReference,
    val additionalInformation: Map<String, Any>? = mapOf(),
) {
    val crn = personReference.identifiers.firstOrNull { it.type == "CRN" }?.value
    val sourceCrn = additionalInformation?.get("sourceCRN") as String?
}

data class PersonReference(
    val identifiers: List<Identifiers>,
)

data class Identifiers(
    val type: String,
    val value: String,
)
