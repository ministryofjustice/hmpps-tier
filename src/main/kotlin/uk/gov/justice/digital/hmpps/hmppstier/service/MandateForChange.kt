package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.client.Requirement
import uk.gov.justice.digital.hmpps.hmppstier.client.Sentence

@Service
class MandateForChange(
  private val communityApiClient: CommunityApiClient
) {
  fun hasNoMandate(crn: String, convictions: Collection<Conviction>): Boolean =
    convictions
      .filter { isCurrent(it.sentence) }
      .none {
        isCustodial(it.sentence) || hasNonRestrictiveRequirements(crn, it.convictionId)
      }.also { log.debug("Has no mandate for change: $it") }

  private fun isCurrent(sentence: Sentence): Boolean =
    sentence.terminationDate == null

  private fun hasNonRestrictiveRequirements(crn: String, convictionId: Long): Boolean =
    communityApiClient.getRequirements(crn, convictionId)
      .filter { excludeUnpaidWork(it) }
      .any { !it.isRestrictive }
      .also { log.debug("Has non-restrictive requirements: $it") }

  private fun excludeUnpaidWork(it: Requirement): Boolean =
    it.mainCategory !in unpaidWorkAndOrderExtended

  companion object {
    fun isCustodial(sentence: Sentence): Boolean =
      sentence.sentenceType.code in custodialSentences

    private val log = LoggerFactory.getLogger(this::class.java)
    private val custodialSentences = arrayOf("NC", "SC")
    private val unpaidWorkAndOrderExtended = arrayOf("W", "W1")
  }
}
