package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.Registration
import uk.gov.justice.digital.hmpps.hmppstier.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusAssessments
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh

@Service
class CommunityApiService(
  private val communityApiClient: CommunityApiClient,
) {

  fun getDeliusAssessments(crn: String): DeliusAssessments =
    DeliusAssessments.from(communityApiClient.getDeliusAssessments(crn))

  fun getConvictionsWithSentences(crn: String): List<Conviction> =
    communityApiClient.getConvictions(crn).filterNot { it.sentence == null }.map { Conviction.from(it) }

  fun getRosh(registrations: Collection<Registration>): Rosh? =
    registrations.mapNotNull { Rosh.from(it.type.code) }.firstOrNull()

  fun getMappa(registrations: Collection<Registration>): Mappa? =
    registrations.mapNotNull { Mappa.from(it.registerLevel?.code) }.firstOrNull()

  fun getComplexityFactors(registrations: Collection<Registration>): Collection<ComplexityFactor> =
    registrations.mapNotNull { ComplexityFactor.from(it.type.code) }.distinct()
}
