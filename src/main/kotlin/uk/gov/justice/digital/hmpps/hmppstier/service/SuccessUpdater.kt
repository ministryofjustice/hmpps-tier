package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.dto.TierDto

@Service
class SuccessUpdater(val client: CommunityApiClient) {
  fun update(tierDto: TierDto, crn: String) {
    println("Doing the update")
    client.updateTier(tierDto, crn)
    println("Update completed")
  }
}
