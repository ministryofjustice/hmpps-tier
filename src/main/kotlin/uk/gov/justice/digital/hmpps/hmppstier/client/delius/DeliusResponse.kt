package uk.gov.justice.digital.hmpps.hmppstier.client.delius

import com.fasterxml.jackson.annotation.JsonCreator
import java.math.BigDecimal
import java.time.LocalDate

/***
 * The response from Tier-To-Delius API
 * @property gender: Person's gender
 * @property registrations: A list of the registrations
 * @property convictions: List of convictions containing sentence type and requirements
 * @property rsrscore: RSR Score
 * @property ogrsscore: OGRS Score
 * @property previousEnforcementActivity: Flag if there is a breach/recall on an active and less-than-a-year conviction.
 */
data class DeliusResponse @JsonCreator constructor(
    val gender: String,
    val registrations: List<DeliusRegistration>,
    val convictions: List<DeliusConviction>,
    val rsrscore: BigDecimal?,
    val ogrsscore: Int?,
    val previousEnforcementActivity: Boolean,
    val latestReleaseDate: LocalDate?,
    val hasActiveEvent: Boolean,
)