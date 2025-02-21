package uk.gov.justice.digital.hmpps.hmppstier.service

sealed interface RecalculationSource {
    val changeReason: String get() = "Automated re-calculation"

    data object FullRecalculation : RecalculationSource

    data object LimitedRecalculation : RecalculationSource

    data object OnDemandRecalculation : RecalculationSource

    data class Other(val type: String?) : RecalculationSource {
        override val changeReason: String = "Unknown"
    }

    sealed interface EventSource : RecalculationSource {
        val type: String

        data class DomainEventRecalculation(override val type: String, override val changeReason: String) : EventSource
    }

    companion object {
        fun of(value: String, eventType: String?, description: String?): RecalculationSource = when (value) {
            FullRecalculation::class.simpleName -> FullRecalculation
            LimitedRecalculation::class.simpleName -> LimitedRecalculation
            OnDemandRecalculation::class.simpleName -> OnDemandRecalculation
            EventSource.DomainEventRecalculation::class.simpleName ->
                EventSource.DomainEventRecalculation(eventType!!, description!!)

            else -> Other(eventType)
        }
    }
}
