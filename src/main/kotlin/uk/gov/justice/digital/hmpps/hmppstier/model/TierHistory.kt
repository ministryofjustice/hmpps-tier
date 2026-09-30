package uk.gov.justice.digital.hmpps.hmppstier.model

import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier
import java.time.LocalDateTime
import java.util.*

interface TierHistory {
    val calculationId: UUID
    val calculationDate: LocalDateTime
    val changeReason: String?
    val tier: Tier?
    val provisional: Boolean?
    val protectLevel: ProtectLevel
    val changeLevel: ChangeLevel
    val unsupervised: Boolean?
}
