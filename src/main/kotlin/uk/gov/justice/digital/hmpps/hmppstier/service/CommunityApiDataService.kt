package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NsiStatus
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.service.exception.EntityNotFoundException
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate

@Service
class CommunityApiDataService(private val communityApiClient: CommunityApiClient, private val clock: Clock) {

  fun getRosh(crn: String): Rosh? {
    return communityApiClient.getRegistrations(crn)
      .filter { it.active }
      .sortedByDescending { it.startDate }
      .mapNotNull { Rosh.from(it.type.code) }
      .firstOrNull()
      .also {
        log.debug("Rosh for $crn: $it")
      }
  }

  fun getMappa(crn: String): Mappa? {
    return communityApiClient.getRegistrations(crn)
      .filter { reg -> reg.active }
      .sortedByDescending { reg -> reg.startDate }
      .mapNotNull { reg -> Mappa.from(reg.registerLevel.code) }
      .firstOrNull()
      .also {
        log.debug("Mappa for $crn: $it")
      }
  }

  fun getComplexityFactors(crn: String): List<ComplexityFactor> {
    return communityApiClient.getRegistrations(crn)
      .filter { it.active }
      .mapNotNull { ComplexityFactor.from(it.type.code) }
      .also {
        log.debug("Complexity Factors for $crn: $it")
      }
  }

  fun getRSR(crn: String): BigDecimal? {
    return communityApiClient.getAssessments(crn)?.rsr
    .also {
      log.debug("RSR for $crn: $it")
    }
  }

  fun getOGRS(crn: String): Int? {
    return communityApiClient.getAssessments(crn)?.ogrs
    .also {
      log.debug("OGRS for $crn: $it")
    }
  }

  fun isFemaleOffender(crn: String): Boolean {
    return getOffenderGender(crn).equals("Female", true)
      .also {
        log.debug("Gender for $crn: $it")
      }
  }

  fun hasBreachedConvictions(crn: String): Boolean {
    val cutoff = LocalDate.now(clock).minusYears(1).minusDays(1)
    val qualifyingConvictions = communityApiClient.getConvictions(crn)
      .filter { it.sentence.terminationDate == null || it.sentence.terminationDate!!.isAfter(cutoff) }
      .map { it.convictionId }
      .also { log.debug("Breach Qualifying Convictions for $crn: ${it.size}") }

    for (convictionId in qualifyingConvictions) {
      val hasBreachOrRecall = communityApiClient.getBreachRecallNsis(crn, convictionId)
        .any { NsiStatus.from(it.status.code) != null }
      if (hasBreachOrRecall) {

        return true
          .also { log.debug("Has breached convictions for $crn: $it") }
      }
    }

    return false
      .also { log.debug("Has breached convictions for $crn: $it") }
  }

  fun hasRestrictiveRequirements(crn: String): Boolean {
    val qualifyingConvictions = currentConvictions(crn)
      .map { it.convictionId }
    for (convictionId in qualifyingConvictions) {
      return communityApiClient.getRequirements(crn, convictionId).any {
        true == it.restrictive
      }.also { log.debug("Has Restrictive Requirements for $crn: $it") }
    }
    return false
      .also { log.debug("Has Restrictive Requirements for $crn: $it") }
  }

  fun hasCurrentCustodialSentence(crn: String): Boolean =
    currentConvictions(crn).any {
      it.sentence.sentenceType.code in custodialSentences
    }.also { log.debug("Has Current Custodial sentence for $crn: $it") }

  fun hasCurrentNonCustodialSentence(crn: String): Boolean =
    currentConvictions(crn).any {
    it.sentence.sentenceType.code !in custodialSentences
  }.also { log.debug("Has Current Non Custodial sentence for $crn: $it") }

  fun hasUnpaidWork(crn: String): Boolean =
    currentConvictions(crn).any {
    null != it.sentence.unpaidWork?.minutesOrdered
  }.also { log.debug("Unpaid work for $crn: $it") }


  private fun getOffenderGender(crn: String): String {
    return communityApiClient.getOffender(crn).gender
      .also { log.debug("Gender for $crn: $it") }
      ?: throw EntityNotFoundException("Offender gender not found")
  }

  private fun currentConvictions(crn: String) = communityApiClient.getConvictions(crn)
    .filter { it.sentence.terminationDate == null }
    .also { log.debug("Non terminated Convictions for $crn: ${it.size}") }

  companion object {
    private val log = LoggerFactory.getLogger(CommunityApiDataService::class.java)
    private val custodialSentences = arrayOf("NC", "SC")
  }
}
