package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh

@Service
class CommunityApiDataService(val communityApiClient: CommunityApiClient) {

  fun getRosh(crn: String): Rosh? {
    val allRegistration = communityApiClient.getRegistrations(crn)
    return allRegistration.filter { it.active }
      .sortedByDescending { it.startDate }
      .mapNotNull { Rosh.from(it.type.code) }
      .firstOrNull()
  }

  fun getMappa(crn: String): Mappa? {
    val allRegistration = communityApiClient.getRegistrations(crn)
    return allRegistration.filter { it.active }
      .sortedByDescending { it.startDate }
      .mapNotNull { Mappa.from(it.registerLevel.code) }
      .firstOrNull()
  }

  fun getComplexityFactors(crn: String): List<ComplexityFactor> {
    val allRegistration = communityApiClient.getRegistrations(crn)
    return allRegistration.filter { it.active }
      .mapNotNull { ComplexityFactor.from(it.type.code) }
  }

}
