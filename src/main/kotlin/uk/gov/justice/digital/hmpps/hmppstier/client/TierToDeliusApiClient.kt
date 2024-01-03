package uk.gov.justice.digital.hmpps.hmppstier.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.math.BigDecimal
import java.time.LocalDate

@Component
class TierToDeliusApiClient(
  @Qualifier("tierToDeliusApiClientWebClientAppScope") private val restClient: RestClient,
  private val objectMapper: ObjectMapper,
) {
  fun getDeliusTier(crn: String): TierToDeliusResponse {
    return restClient
      .get()
      .uri("/tier-details/$crn")
      .exchange { req, res ->
        when (res.statusCode) {
          HttpStatus.OK -> objectMapper.readValue<TierToDeliusResponse>(res.body)
          HttpStatus.NOT_FOUND -> throw HttpClientErrorException(res.statusCode, "Not Found from GET ${req.uri}")
          else -> throw HttpClientErrorException(res.statusCode, res.statusText)
        }
      }
  }

  fun getActiveCrns(): List<String> = restClient
    .get()
    .uri("/probation-cases")
    .accept(APPLICATION_JSON)
    .retrieve()
    .body<List<String>>()!!
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
