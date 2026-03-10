package uk.gov.justice.digital.hmpps.hmppstier.service.calculation

import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusConviction
import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusRequirement

object MandateForChange {
    private val unpaidWorkAndOrderExtended = arrayOf("W", "W1")
    private val custodialSentenceTypes = arrayOf("NC", "SC")

    fun Collection<DeliusConviction>.hasNoMandate(): Boolean = filter { isCurrent(it) }
        .none {
            isCustodial(it) || hasNonRestrictiveRequirements(it.requirements)
        }

    private fun isCurrent(conviction: DeliusConviction): Boolean =
        conviction.terminationDate == null

    private fun isCustodial(conviction: DeliusConviction): Boolean =
        conviction.sentenceTypeCode in custodialSentenceTypes

    private fun hasNonRestrictiveRequirements(requirement: List<DeliusRequirement>): Boolean =
        requirement
            .filter { excludeUnpaidWork(it) }
            .any { isNonRestrictive(it) }

    private fun isNonRestrictive(it: DeliusRequirement) = !it.restrictive

    private fun excludeUnpaidWork(it: DeliusRequirement): Boolean =
        it.mainCategoryTypeCode !in unpaidWorkAndOrderExtended
}