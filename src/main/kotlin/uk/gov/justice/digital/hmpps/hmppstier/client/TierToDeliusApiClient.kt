package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.annotation.JsonCreator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZonedDateTime

@Component
class TierToDeliusApiClient(@Qualifier("tierToDeliusApiClientWebClientAppScope") private val webClient: WebClient) {
  suspend fun getDeliusTier(crn: String): TierToDeliusResponse {
    return webClient
      .get()
      .uri("/tier-details/$crn")
      .retrieve()
      .awaitBody()
  }
}

data class TierToDeliusResponse @JsonCreator constructor(
  val gender: String,
  val currentTier: String,
  val registrations: List<DeliusRegistration>,
  val convictions: List<DeliusConviction>,
  val rsrscore: BigDecimal?,
  val ogrsscore: Int?,
)

data class DeliusRegistration @JsonCreator constructor(
  val code: String,
  val description: String,
  val level: String?,
  val date: LocalDate,
)

data class DeliusConviction @JsonCreator constructor(
  val terminationDate: ZonedDateTime?,
  val sentenceTypeCode: String,
  val sentenceTypeDescription: String,
  val breached: Boolean,
  val requirements: List<DeliusRequirement>,
)

data class DeliusRequirement @JsonCreator constructor(
  val mainCategoryTypeCode: String,
  val restrictive: Boolean,
)
