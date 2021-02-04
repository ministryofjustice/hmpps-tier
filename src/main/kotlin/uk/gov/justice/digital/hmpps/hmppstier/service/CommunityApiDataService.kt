package uk.gov.justice.digital.hmpps.hmppstier.service

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
  }

  fun getMappa(crn: String): Mappa? {
    return communityApiClient.getRegistrations(crn)
      .filter { reg -> reg.active }
      .sortedByDescending { reg -> reg.startDate }
      .mapNotNull { reg -> Mappa.from(reg.registerLevel.code) }
      .firstOrNull()
  }

  fun getComplexityFactors(crn: String): List<ComplexityFactor> {
    return communityApiClient.getRegistrations(crn)
      .filter { it.active }
      .mapNotNull { ComplexityFactor.from(it.type.code) }
  }

  fun getRSR(crn: String): BigDecimal? {
    return communityApiClient.getAssessments(crn)?.rsr
  }

  fun getOGRS(crn: String): Int? {
    return communityApiClient.getAssessments(crn)?.ogrs
  }

  fun isFemaleOffender(crn: String): Boolean {
    return getOffenderGender(crn).equals("Female", true)
  }

  fun hasBreachedConvictions(crn: String): Boolean {
    val cutoff = LocalDate.now(clock).minusYears(1).minusDays(1)
    val breachConvictionIds = communityApiClient.getConvictions(crn)
      .filter { it.sentence.terminationDate == null || it.sentence.terminationDate!!.isAfter(cutoff) }
      .map { it.convictionId }

    for (convictionId in breachConvictionIds) {
      val hasBreachOrRecall = communityApiClient.getBreachRecallNsis(crn, convictionId)
        .any { NsiStatus.from(it.status.code) != null }
      if (hasBreachOrRecall) {
        return true
      }
    }

    return false
  }

  fun hasRestrictiveRequirements(crn: String): Boolean {
    val currentConvictions = communityApiClient.getConvictions(crn)
      .filter { it.sentence.terminationDate == null }
      .map { it.convictionId }
    for (convictionId in currentConvictions) {
      if (communityApiClient.getRequirements(crn, convictionId).any {
        true == it.restrictive
      }
      ) {
        return true
      }
    }
    return false
  }

  fun isCustodialSentence(crn: String): Boolean {
    return communityApiClient.getConvictions(crn).any {
      it.sentence.sentenceType.code in arrayOf("NC", "SC")
    }
  }

  private fun getOffenderGender(crn: String): String {
    return communityApiClient.getOffender(crn)?.gender
      ?: throw EntityNotFoundException("Offender or Offender gender not found")
  }
}
