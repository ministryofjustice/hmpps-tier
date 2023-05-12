package uk.gov.justice.digital.hmpps.hmppstier.service

import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusConviction
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusRequirement

class MandateForChange {
  fun hasNoMandate(convictions: Collection<DeliusConviction>): Boolean =
    convictions
      .filter { isCurrent(it) }
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

  companion object {
    private val unpaidWorkAndOrderExtended = arrayOf("W", "W1")
    private val custodialSentenceTypes = arrayOf("NC", "SC")
  }
}
