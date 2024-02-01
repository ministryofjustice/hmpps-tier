package uk.gov.justice.digital.hmpps.hmppstier.service

sealed interface RecalculationSource {
    data object FullRecalculation : RecalculationSource

    data object LimitedRecalculation : RecalculationSource

    data class Other(val type: String?) : RecalculationSource

    sealed interface EventSource : RecalculationSource {
        val type: String

        data class DomainEventRecalculation(override val type: String) : EventSource
        data class OffenderEventRecalculation(override val type: String) : EventSource
    }

    companion object {
        fun of(value: String, eventType: String?): RecalculationSource = when (value) {
            FullRecalculation::class.simpleName -> FullRecalculation
            LimitedRecalculation::class.simpleName -> LimitedRecalculation
            EventSource.OffenderEventRecalculation::class.simpleName -> EventSource.OffenderEventRecalculation(eventType!!)
            EventSource.DomainEventRecalculation::class.simpleName -> EventSource.DomainEventRecalculation(eventType!!)
            else -> Other(eventType)
        }
    }
}
