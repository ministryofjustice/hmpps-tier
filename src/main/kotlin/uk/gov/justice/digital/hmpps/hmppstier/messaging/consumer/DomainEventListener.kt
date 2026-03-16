package uk.gov.justice.digital.hmpps.hmppstier.messaging.consumer

import io.awspring.cloud.sqs.annotation.SqsListener
import io.awspring.cloud.sqs.listener.AsyncAdapterBlockingExecutionFailedException
import io.awspring.cloud.sqs.listener.ListenerExecutionFailedException
import io.sentry.Sentry
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.dao.CannotAcquireLockException
import org.springframework.jdbc.CannotGetJdbcConnectionException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.CannotCreateTransactionException
import org.springframework.transaction.UnexpectedRollbackException
import org.springframework.web.client.RestClientException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.util.retry.Retry
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.hmppstier.config.ReactorExtensions.invoke
import uk.gov.justice.digital.hmpps.hmppstier.domain.RecalculationSource
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService
import java.time.Duration
import java.util.concurrent.CompletionException

@Service
@ConditionalOnProperty("messaging.consumer.enabled", matchIfMissing = true)
class DomainEventListener(
    private val calculator: TierCalculationService,
    private val objectMapper: ObjectMapper,
) {

    @SqsListener("hmppsdomaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
    fun listen(msg: String) {
        val (message, attributes) = objectMapper.readValue<SQSMessage>(msg)
        val domainEventMessage = objectMapper.readValue<DomainEvent>(message)
        if (attributes.eventType == "risk-assessment.scores.determined" && domainEventMessage.eventType != "assessment.summary.produced") {
            return
        }
        try {
            Retry.backoff(3, Duration.ofMillis(300))
                .filter { e -> RETRYABLE_EXCEPTIONS.any { it.isInstance(e) } }
                .invoke {
                    handleMessage(domainEventMessage)
                }
        } catch (e: Exception) {
            Sentry.captureException(unwrapSqsExceptions(e))
            throw e
        }
    }

    private fun handleMessage(message: DomainEvent) {
        when (message.eventType) {
            "probation-case.deleted.gdpr" -> message.crn?.also {
                calculator.deleteCalculationsForCrn(it, message.eventType)
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
        calculator.calculateTierForCrn(crn, source)
    }

    companion object {
        val RETRYABLE_EXCEPTIONS = listOf(
            RestClientException::class,
            WebClientResponseException::class,
            CannotAcquireLockException::class,
            ObjectOptimisticLockingFailureException::class,
            CannotCreateTransactionException::class,
            CannotGetJdbcConnectionException::class,
            UnexpectedRollbackException::class
        )

        fun unwrapSqsExceptions(e: Throwable): Throwable {
            fun unwrap(e: Throwable) = e.cause ?: e
            var cause = e
            if (cause is CompletionException) {
                cause = unwrap(cause)
            }
            if (cause is AsyncAdapterBlockingExecutionFailedException) {
                cause = unwrap(cause)
            }
            if (cause is ListenerExecutionFailedException) {
                cause = unwrap(cause)
            }
            return cause
        }
    }
}
