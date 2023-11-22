package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.annotation.JsonCreator
import kotlinx.coroutines.flow.Flow
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.bodyToFlow
import org.springframework.web.reactive.function.client.exchangeToFlow
import java.math.BigDecimal
import java.time.LocalDate

@Component
class TierToDeliusApiClient(@Qualifier("tierToDeliusApiClientWebClientAppScope") private val webClient: WebClient) {
  suspend fun getDeliusTier(crn: String): TierToDeliusResponse {
    return webClient
      .get()
      .uri("/tier-details/$crn")
      .retrieve()
      .awaitBody()
  }

  fun getActiveCrns(): Flow<String> = webClient
    .get()
    .uri("/probation-cases")
    .accept(TEXT_PLAIN)
    .exchangeToFlow {
      it.bodyToFlow<String>()
    }
}

/***
 * The response from Tier-To-Delius API
 * @property gender: Person's gender
 * @property registrations: A list of the registrations
 * @property convictions: List of convictions containing sentence type and requirements
 * @property rsrscore: RSR Score
 * @property ogrsscore: OGRS Score
 * @property previousEnforcementActivity: Flag if there is a breach/recall on an active and less-than-a-year conviction.
 */
data class TierToDeliusResponse @JsonCreator constructor(
  val gender: String,
  val registrations: List<DeliusRegistration>,
  val convictions: List<DeliusConviction>,
  val rsrscore: BigDecimal?,
  val ogrsscore: Int?,
  val previousEnforcementActivity: Boolean,
)

data class DeliusRegistration @JsonCreator constructor(
  val code: String,
  val level: String?,
  val date: LocalDate,
)

data class DeliusConviction @JsonCreator constructor(
  val terminationDate: LocalDate?,
  val sentenceTypeCode: String,
  val requirements: List<DeliusRequirement>,
)

data class DeliusRequirement @JsonCreator constructor(
  val mainCategoryTypeCode: String,
  val restrictive: Boolean,
)
