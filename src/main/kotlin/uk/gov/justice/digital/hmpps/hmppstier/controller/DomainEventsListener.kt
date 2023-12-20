package uk.gov.justice.digital.hmpps.hmppstier.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.service.RecalculationSource
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService

@Service
class DomainEventsListener(
    private val calculator: TierCalculationService,
    private val objectMapper: ObjectMapper,
) {

    @SqsListener("hmppsdomaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
    fun listen(msg: String) = runBlocking {
        getCrn(msg)?.also {
            calculator.calculateTierForCrn(it, RecalculationSource.DomainEventRecalculation)
        }
    }

    private fun getCrn(msg: String): String? {
        val (message) = objectMapper.readValue<SQSMessage>(msg)
        val domainEventMessage = objectMapper.readValue<DomainEventsMessage>(message)
        return if (domainEventMessage.eventType in messageTypesOfInterest) {
            val crn = domainEventMessage.personReference.identifiers.first { it.type == "CRN" }.value
            log.info("Domain event received of type ${domainEventMessage.eventType} and CRN: $crn")
            crn
        } else null
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)

        private val messageTypesOfInterest = listOf(
            "assessment.summary.produced",
            "enforcement.breach.raised",
            "enforcement.breach.concluded",
            "enforcement.recall.raised",
            "enforcement.recall.concluded",
            "probation-case.registration.added",
            "probation-case.registration.updated",
            "probation-case.registration.deleted",
            "probation-case.registration.deregistered",
        )
    }
}

data class DomainEventsMessage(
    val eventType: String,
    val personReference: PersonReference,
)

data class PersonReference(
    val identifiers: List<Identifiers>,
)

data class Identifiers(
    val type: String,
    val value: String,
)
