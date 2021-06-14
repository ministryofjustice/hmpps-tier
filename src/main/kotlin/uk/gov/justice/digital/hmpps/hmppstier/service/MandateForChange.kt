package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.Requirement
import uk.gov.justice.digital.hmpps.hmppstier.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.domain.Sentence

@Service
class MandateForChange(
  private val communityApiService: CommunityApiService
) {
  fun hasNoMandate(crn: String, convictions: Collection<Conviction>): Boolean =
    convictions
      .filter { isCurrent(it.sentence) }
      .none {
        it.sentence.isCustodial() || hasNonRestrictiveRequirements(crn, it.convictionId)
      }

  private fun isCurrent(sentence: Sentence): Boolean =
    sentence.terminationDate == null

  private fun hasNonRestrictiveRequirements(crn: String, convictionId: Long): Boolean =
    communityApiService.getRequirements(crn, convictionId)
      .filter { excludeUnpaidWork(it) }
      .any { isNonRestrictive(it) }

  private fun isNonRestrictive(it: Requirement) = !it.isRestrictive

  private fun excludeUnpaidWork(it: Requirement): Boolean =
    it.mainCategory !in unpaidWorkAndOrderExtended

  companion object {
    private val unpaidWorkAndOrderExtended = arrayOf("W", "W1")
  }
}
