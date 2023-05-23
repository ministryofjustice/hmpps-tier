package uk.gov.justice.digital.hmpps.hmppstier.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.CalculationRule
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.CommunityApiExtension.Companion.communityApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.tierToDeliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Registration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Requirement
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.TierDetails
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class DeliusCommunityDataReliabilityTest(@Autowired val repository: TierCalculationRepository) : IntegrationTestBase() {
  @BeforeEach
  fun resetDatabase() {
    repository.deleteAll()
  }

  val created: LocalDateTime = LocalDateTime.now()
  val crn1 = "X123457"
  val crn2 = "X123456"
  val ogrsScore = "21"
  val rsrScore = "23"

  @Test
  fun `delius assessments data are identical per crn`() {
    communityApi.getAssessmentResponse(crn1)
    communityApi.getOffender(crn1, "female", "A01")
    communityApi.getConviction(crn1)
    communityApi.getRequirement(crn1)
    communityApi.getRegistration(crn1)
    communityApi.getNsi(crn1, 12345)
    tierToDeliusApi.getFullDetails(
      crn1,
      TierDetails(
        ogrsScore = ogrsScore,
        rsrScore = rsrScore,
        convictions = listOf(Conviction(true, mutableListOf(Requirement("X", false)), "NC", LocalDate.of(2021, 2, 1))),
        registrations = listOf(
          Registration("M1", "MAPP", LocalDate.of(2021, 2, 1)),
        ),
      ),
    )

    val response = webTestClient.get()
      .uri("/crn/$crn1")
      .headers { it.authToken(roles = listOf("ROLE_HMPPS_TIER")) }
      .exchange()
      .expectStatus().isOk
      .expectBody<CommunityDeliusData>()
      .returnResult()
      .responseBody

    assertThat(response!!.crn).isEqualTo(crn1)
    assertThat(response.rsrMatch).isEqualTo(true)
    assertThat(response.ogrsMatch).isEqualTo(true)
    assertThat(response.rsrDelius).isEqualTo(BigDecimal.valueOf(23))
    assertThat(response.rsrCommunity).isEqualTo(BigDecimal.valueOf(23.0))
    assertThat(response.ogrsDelius).isEqualTo(2)
    assertThat(response.ogrsCommunity).isEqualTo(2)
    assertThat(response.convictionsMatch).isEqualTo(true)
    assertThat(response.registrationMatch).isEqualTo(true)
  }

  @Test
  fun `no delius assessments data`() {
    communityApi.getEmptyAssessmentResponse(crn1)
    communityApi.getOffender(crn1, "female", "A01")
    communityApi.getConviction(crn1)
    communityApi.getRequirement(crn1)
    communityApi.getRegistration(crn1)
    communityApi.getNsi(crn1, 12345)

    tierToDeliusApi.getFullDetails(crn1, TierDetails(ogrsScore = null, rsrScore = null))

    val response = webTestClient.get()
      .uri("/crn/$crn1")
      .headers { it.authToken(roles = listOf("ROLE_HMPPS_TIER")) }
      .exchange()
      .expectStatus().isOk
      .expectBody<CommunityDeliusData>()
      .returnResult()
      .responseBody

    assertThat(response!!.crn).isEqualTo(crn1)
    assertThat(response.rsrMatch).isEqualTo(true)
    assertThat(response.ogrsMatch).isEqualTo(true)
    assertThat(response.rsrDelius).isEqualTo(BigDecimal.valueOf(0))
    assertThat(response.rsrCommunity).isEqualTo(BigDecimal.valueOf(0))
    assertThat(response.ogrsDelius).isEqualTo(0)
    assertThat(response.ogrsCommunity).isEqualTo(0)
  }

  @Test
  fun `no delius currentTier data`() {
    communityApi.getEmptyAssessmentResponse(crn1)
    communityApi.getOffender(crn1, "female", "A01")
    communityApi.getConviction(crn1)
    communityApi.getRequirement(crn1)
    communityApi.getRegistration(crn1)
    communityApi.getNsi(crn1, 12345)

    tierToDeliusApi.getFullDetails(crn1, TierDetails(currentTier = null, ogrsScore = null, rsrScore = null))

    val response = webTestClient.get()
      .uri("/crn/$crn1")
      .headers { it.authToken(roles = listOf("ROLE_HMPPS_TIER")) }
      .exchange()
      .expectStatus().isOk
      .expectBody<CommunityDeliusData>()
      .returnResult()
      .responseBody

    assertThat(response!!.crn).isEqualTo(crn1)
  }

  @Test
  fun `delius and community assessments do not match`() {
    communityApi.getAssessmentResponse(crn1)
    communityApi.getOffender(crn1, "female", "A01")
    communityApi.getConviction(crn1)
    communityApi.getRequirement(crn1)
    communityApi.getRegistration(crn1)
    communityApi.getNsi(crn1, 12345)

    tierToDeliusApi.getFullDetails(crn1, TierDetails(ogrsScore = null, rsrScore = null))

    val response = webTestClient.get()
      .uri("/crn/$crn1")
      .headers { it.authToken(roles = listOf("ROLE_HMPPS_TIER")) }
      .exchange()
      .expectStatus().isOk
      .expectBody<CommunityDeliusData>()
      .returnResult()
      .responseBody

    assertThat(response!!.crn).isEqualTo(crn1)
    assertThat(response.rsrMatch).isEqualTo(false)
    assertThat(response.ogrsMatch).isEqualTo(false)
    assertThat(response.rsrDelius).isEqualTo(BigDecimal.ZERO)
    assertThat(response.rsrCommunity).isEqualTo(BigDecimal.valueOf(23.0))
    assertThat(response.ogrsDelius).isEqualTo(0)
    assertThat(response.ogrsCommunity).isEqualTo(2)
  }

  @Test
  fun `delius and community breach do not match`() {
    communityApi.getAssessmentResponse(crn1)
    communityApi.getOffender(crn1, "female", "A01")
    communityApi.getConviction(crn1)
    communityApi.getRequirement(crn1)
    communityApi.getRegistration(crn1)
    communityApi.getNsi(crn1, 12345, null)
    tierToDeliusApi.getFullDetails(
      crn1,
      TierDetails(
        convictions = listOf(Conviction(true, mutableListOf(Requirement("X", false)), "NC", LocalDate.of(2021, 2, 1))),
      ),
    )
    val response = webTestClient.get()
      .uri("/crn/$crn1")
      .headers { it.authToken(roles = listOf("ROLE_HMPPS_TIER")) }
      .exchange()
      .expectStatus().isOk
      .expectBody<CommunityDeliusData>()
      .returnResult()
      .responseBody

    assertThat(response!!.crn).isEqualTo(crn1)
    assertThat(response.convictionsMatch).isEqualTo(false)
  }

  @Test
  fun `Not found delius response`() {
    communityApi.getAssessmentResponse(crn1)
    communityApi.getOffender(crn1, "female", "A01")
    communityApi.getConviction(crn1)
    communityApi.getRequirement(crn1)
    communityApi.getRegistration(crn1)
    communityApi.getNsi(crn1, 12345, null)
    tierToDeliusApi.getNotFound(crn1)

    val response = webTestClient.get()
      .uri("/crn/$crn1")
      .headers { it.authToken(roles = listOf("ROLE_HMPPS_TIER")) }
      .exchange()
      .expectStatus().isOk
      .expectBody<CommunityDeliusData>()
      .returnResult()
      .responseBody

    assertThat(response!!.crn).isEqualTo(crn1)
    assertThat(response.genderMatch).isEqualTo(false)
    assertThat(response.convictionsMatch).isEqualTo(false)
    assertThat(response.registrationMatch).isEqualTo(false)
    assertThat(response.ogrsDelius).isEqualTo(0)
  }

  @Test
  fun `Not found community offender`() {
    communityApi.getAssessmentResponse(crn1)
    communityApi.getNotFoundOffender(crn1)
    communityApi.getConviction(crn1)
    communityApi.getRequirement(crn1)
    communityApi.getRegistration(crn1)
    communityApi.getNsi(crn1, 12345, null)
    tierToDeliusApi.getFullDetails(crn1, TierDetails(ogrsScore = null, rsrScore = null))

    val response = webTestClient.get()
      .uri("/crn/$crn1")
      .headers { it.authToken(roles = listOf("ROLE_HMPPS_TIER")) }
      .exchange()
      .expectStatus().isOk
      .expectBody<CommunityDeliusData>()
      .returnResult()
      .responseBody

    assertThat(response!!.crn).isEqualTo(crn1)
    assertThat(response.genderMatch).isEqualTo(false)
  }

  @Test
  fun `Empty community registration response`() {
    communityApi.getAssessmentResponse(crn1)
    communityApi.getOffender(crn1, "female", "A01")
    communityApi.getConviction(crn1)
    communityApi.getRequirement(crn1)
    communityApi.getEmptyRegistration(crn1)
    communityApi.getNsi(crn1, 12345)
    tierToDeliusApi.getFullDetails(
      crn1,
      TierDetails(
        ogrsScore = ogrsScore,
        rsrScore = rsrScore,
        convictions = listOf(Conviction(true, mutableListOf(Requirement("X", false)), "NC", LocalDate.of(2021, 2, 1))),
        registrations = listOf(
          Registration("M1", "MAPP", LocalDate.of(2021, 2, 1)),
        ),
      ),
    )

    val response = webTestClient.get()
      .uri("/crn/$crn1")
      .headers { it.authToken(roles = listOf("ROLE_HMPPS_TIER")) }
      .exchange()
      .expectStatus().isOk
      .expectBody<CommunityDeliusData>()
      .returnResult()
      .responseBody

    assertThat(response!!.crn).isEqualTo(crn1)
    assertThat(response.registrationMatch).isEqualTo(false)
    assertThat(response.convictionsMatch).isEqualTo(true)
  }

  @Test
  fun `Should only return non-matching reliability for distinct crns`() {
    val firstTierCalculation = TierCalculationEntity(crn = crn1, created = created, data = data, uuid = UUID.randomUUID())
    val secondTierCalculation = TierCalculationEntity(crn = crn2, created = created.minusHours(1), data = data, uuid = UUID.randomUUID())
    val thirdTierCalculation = TierCalculationEntity(crn = crn2, created = created.minusDays(1), data = data, uuid = UUID.randomUUID())
    repository.saveAll(listOf(firstTierCalculation, secondTierCalculation, thirdTierCalculation))

    communityApi.getAssessmentResponse(crn1)
    communityApi.getAssessmentResponse(crn2)
    communityApi.getOffender(crn1, "female", "A01")
    communityApi.getOffender(crn2, "female", "A01")
    communityApi.getConviction(crn1)
    communityApi.getConviction(crn2)
    communityApi.getRequirement(crn1)
    communityApi.getRequirement(crn2)
    communityApi.getRegistration(crn1)
    communityApi.getRegistration(crn2)
    communityApi.getNsi(crn1, 12345)
    communityApi.getNsi(crn2, 12345)

    tierToDeliusApi.getFullDetails(crn1, TierDetails(ogrsScore = ogrsScore, rsrScore = rsrScore))
    tierToDeliusApi.getFullDetails(crn2, TierDetails(ogrsScore = ogrsScore, rsrScore = rsrScore))

    val response = webTestClient.get()
      .uri("/crn/all/0/10")
      .headers { it.authToken(roles = listOf("ROLE_HMPPS_TIER")) }
      .exchange()
      .expectStatus().isOk
      .expectBody<List<CommunityDeliusData>>()
      .returnResult()
      .responseBody

    assertThat(response!!.size).isEqualTo(0)
  }

  @Test
  fun `Should return failed reliability for different ogrs and rsr`() {
    val firstTierCalculation = TierCalculationEntity(crn = crn1, created = created, data = data, uuid = UUID.randomUUID())
    val secondTierCalculation = TierCalculationEntity(crn = crn2, created = created, data = data, uuid = UUID.randomUUID())
    repository.saveAll(listOf(firstTierCalculation, secondTierCalculation))
    communityApi.getAssessmentResponse(crn1)
    communityApi.getAssessmentResponse(crn2)
    communityApi.getOffender(crn1, "female", "A01")
    communityApi.getOffender(crn2, "female", "A01")
    communityApi.getConviction(crn1)
    communityApi.getConviction(crn2)
    communityApi.getRequirement(crn1)
    communityApi.getRequirement(crn2)
    communityApi.getRegistration(crn1)
    communityApi.getRegistration(crn2)
    communityApi.getNsi(crn1, 12345)
    communityApi.getNsi(crn2, 12345)

    tierToDeliusApi.getFullDetails(crn1, TierDetails(ogrsScore = ogrsScore, rsrScore = rsrScore))
    tierToDeliusApi.getFullDetails(crn2, TierDetails(ogrsScore = "0", rsrScore = "0"))

    val response = webTestClient.get()
      .uri("/crn/all/0/10")
      .headers { it.authToken(roles = listOf("ROLE_HMPPS_TIER")) }
      .exchange()
      .expectStatus().isOk
      .expectBody<List<CommunityDeliusData>>()
      .returnResult().responseBody

    assertThat(response!!.size).isEqualTo(1)
    assertThat(response[0].crn).isEqualTo(crn2)
    assertThat(response[0].rsrMatch).isEqualTo(false)
    assertThat(response[0].ogrsMatch).isEqualTo(false)
    assertThat(response[0].rsrDelius).isEqualTo(BigDecimal.ZERO)
    assertThat(response[0].ogrsDelius).isEqualTo(0)
    assertThat(response[0].rsrCommunity).isEqualTo(BigDecimal.valueOf(23.0))
    assertThat(response[0].ogrsCommunity).isEqualTo(2)
  }

  @Test
  fun `Should return failed reliability when either rsr or ogrs do not match`() {
    val firstTierCalculation = TierCalculationEntity(crn = crn1, created = created, data = data, uuid = UUID.randomUUID())
    val secondTierCalculation = TierCalculationEntity(crn = crn2, created = created, data = data, uuid = UUID.randomUUID())
    repository.saveAll(listOf(firstTierCalculation, secondTierCalculation))
    communityApi.getAssessmentResponse(crn1)
    communityApi.getAssessmentResponse(crn2)
    communityApi.getOffender(crn1, "male", "A01")
    communityApi.getOffender(crn2, "female", "A01")
    communityApi.getConviction(crn1)
    communityApi.getConviction(crn2)
    communityApi.getRequirement(crn1)
    communityApi.getRequirement(crn2)
    communityApi.getRegistration(crn1)
    communityApi.getRegistration(crn2)
    communityApi.getNsi(crn1, 12345)
    communityApi.getNsi(crn2, 12345)

    tierToDeliusApi.getFullDetails(crn1, TierDetails(ogrsScore = ogrsScore, rsrScore = "0"))
    tierToDeliusApi.getFullDetails(crn2, TierDetails(ogrsScore = "0", rsrScore = rsrScore))

    val response = webTestClient.get()
      .uri("/crn/all/0/10")
      .headers { it.authToken(roles = listOf("ROLE_HMPPS_TIER")) }
      .exchange()
      .expectStatus().isOk
      .expectBody<List<CommunityDeliusData>>()
      .returnResult().responseBody

    assertThat(response!!.size).isEqualTo(2)
    assertThat(response[0].crn).isEqualTo(crn1)
    assertThat(response[0].rsrMatch).isEqualTo(false)
    assertThat(response[0].ogrsMatch).isEqualTo(true)
    assertThat(response[0].rsrDelius).isEqualTo(BigDecimal.ZERO)
    assertThat(response[0].genderMatch).isEqualTo(true)
    assertThat(response[1].crn).isEqualTo(crn2)
    assertThat(response[1].rsrMatch).isEqualTo(true)
    assertThat(response[1].ogrsMatch).isEqualTo(false)
    assertThat(response[1].ogrsDelius).isEqualTo(0)
    assertThat(response[1].genderMatch).isEqualTo(false)
  }

  @Test
  fun `Should use limit and offset to query the crns`() {
    val firstTierCalculation = TierCalculationEntity(crn = crn1, created = created, data = data, uuid = UUID.randomUUID())
    val secondTierCalculation = TierCalculationEntity(crn = crn2, created = created, data = data, uuid = UUID.randomUUID())
    repository.saveAll(listOf(firstTierCalculation, secondTierCalculation))
    communityApi.getAssessmentResponse(crn1)
    communityApi.getAssessmentResponse(crn2)
    communityApi.getOffender(crn1, "female", "A01")
    communityApi.getOffender(crn2, "female", "A01")
    communityApi.getConviction(crn1)
    communityApi.getConviction(crn2)
    communityApi.getRequirement(crn1)
    communityApi.getRequirement(crn2)
    communityApi.getRegistration(crn1)
    communityApi.getRegistration(crn2)
    communityApi.getNsi(crn1, 12345)
    communityApi.getNsi(crn2, 12345)

    tierToDeliusApi.getFullDetails(crn1, TierDetails(ogrsScore = ogrsScore, rsrScore = "0"))
    tierToDeliusApi.getFullDetails(crn2, TierDetails(ogrsScore = "0", rsrScore = rsrScore))

    val response = webTestClient.get()
      .uri("/crn/all/0/1")
      .headers { it.authToken(roles = listOf("ROLE_HMPPS_TIER")) }
      .exchange()
      .expectStatus().isOk
      .expectBody<List<CommunityDeliusData>>()
      .returnResult().responseBody

    assertThat(response!!.size).isEqualTo(1)
  }

  @Test
  fun `Delius assessments replaced with zero when returned as null`() {
    val tierCalculation = TierCalculationEntity(crn = crn1, created = created, data = data, uuid = UUID.randomUUID())
    repository.save(tierCalculation)
    communityApi.getAssessmentResponse(crn1)
    communityApi.getOffender(crn1, "male", "A01")
    communityApi.getConviction(crn1)
    communityApi.getRequirement(crn1)
    communityApi.getRegistration(crn1)
    communityApi.getRegistration(crn2)
    communityApi.getNsi(crn1, 12345)
    communityApi.getNsi(crn2, 12345)

    tierToDeliusApi.getFullDetails(crn1, TierDetails(ogrsScore = null, rsrScore = null))

    val response = webTestClient.get()
      .uri("/crn/all/0/2")
      .headers { it.authToken(roles = listOf("ROLE_HMPPS_TIER")) }
      .exchange()
      .expectStatus().isOk
      .expectBody<List<CommunityDeliusData>>()
      .returnResult().responseBody

    assertThat(response!!.size).isEqualTo(1)
    assertThat(response[0].crn).isEqualTo(crn1)
    assertThat(response[0].rsrCommunity).isEqualTo(BigDecimal.valueOf(23.0))
    assertThat(response[0].rsrDelius).isEqualTo(BigDecimal.ZERO)
    assertThat(response[0].ogrsCommunity).isEqualTo(2)
    assertThat(response[0].ogrsDelius).isEqualTo(0)
    assertThat(response[0].genderMatch).isEqualTo(true)
  }

  @Test
  fun `Tier to Delius Not found`() {
    val firstTierCalculation = TierCalculationEntity(crn = crn1, created = created, data = data, uuid = UUID.randomUUID())
    val secondTierCalculation = TierCalculationEntity(crn = crn2, created = created, data = data, uuid = UUID.randomUUID())
    repository.saveAll(listOf(firstTierCalculation, secondTierCalculation))
    communityApi.getAssessmentResponse(crn1, "22", "30")
    communityApi.getAssessmentResponse(crn2)
    communityApi.getOffender(crn1, "female", "A01")
    communityApi.getOffender(crn2, "female", "A01")
    communityApi.getConviction(crn1)
    communityApi.getConviction(crn2)
    communityApi.getRequirement(crn1)
    communityApi.getRequirement(crn2)
    communityApi.getRegistration(crn1)
    communityApi.getRegistration(crn2)
    communityApi.getNsi(crn1, 12345)
    communityApi.getNsi(crn2, 12345)

    tierToDeliusApi.getFullDetails(crn1, TierDetails(ogrsScore = ogrsScore, rsrScore = rsrScore))
    tierToDeliusApi.getNotFound(crn2)

    val response = webTestClient.get()
      .uri("/crn/all/0/10")
      .headers { it.authToken(roles = listOf("ROLE_HMPPS_TIER")) }
      .exchange()
      .expectStatus().isOk
      .expectBody<List<CommunityDeliusData>>()
      .returnResult().responseBody

    assertThat(response!!.size).isEqualTo(1)
    assertThat(response[0].crn).isEqualTo(crn1)
    assertThat(response[0].rsrCommunity).isEqualTo(BigDecimal.valueOf(22))
    assertThat(response[0].rsrDelius).isEqualTo(BigDecimal.valueOf(23))
    assertThat(response[0].ogrsCommunity).isEqualTo(3)
    assertThat(response[0].ogrsDelius).isEqualTo(2)
  }

  @Test
  fun `Empty community convictions response`() {
    val firstTierCalculation = TierCalculationEntity(crn = crn1, created = created, data = data, uuid = UUID.randomUUID())
    val secondTierCalculation = TierCalculationEntity(crn = crn2, created = created, data = data, uuid = UUID.randomUUID())
    repository.saveAll(listOf(firstTierCalculation, secondTierCalculation))
    communityApi.getAssessmentResponse(crn1)
    communityApi.getAssessmentResponse(crn2)
    communityApi.getOffender(crn1, "female", "A01")
    communityApi.getOffender(crn2, "female", "A01")
    communityApi.getConviction(crn1, null)
    communityApi.getConviction(crn2, null)
    communityApi.getRequirement(crn1)
    communityApi.getRequirement(crn2)
    communityApi.getRegistration(crn1)
    communityApi.getRegistration(crn2)
    communityApi.getNsi(crn1, 12345)
    communityApi.getNsi(crn2, 12345)

    tierToDeliusApi.getFullDetails(crn1, TierDetails(ogrsScore = ogrsScore, rsrScore = rsrScore))
    tierToDeliusApi.getNotFound(crn2)

    val response = webTestClient.get()
      .uri("/crn/all/0/10")
      .headers { it.authToken(roles = listOf("ROLE_HMPPS_TIER")) }
      .exchange()
      .expectStatus().isOk
      .expectBody<List<CommunityDeliusData>>()
      .returnResult().responseBody

    assertThat(response!!.size).isEqualTo(0)
  }

  @Test
  fun `Get not found CRNs`() {
    val firstTierCalculation = TierCalculationEntity(crn = crn1, created = created, data = data, uuid = UUID.randomUUID())
    val secondTierCalculation = TierCalculationEntity(crn = crn2, created = created, data = data, uuid = UUID.randomUUID())
    repository.saveAll(listOf(firstTierCalculation, secondTierCalculation))

    tierToDeliusApi.getFullDetails(crn1, TierDetails(ogrsScore = ogrsScore, rsrScore = rsrScore))
    tierToDeliusApi.getNotFound(crn2)

    val response = webTestClient.get()
      .uri("/crn/0/10")
      .headers { it.authToken(roles = listOf("ROLE_HMPPS_TIER")) }
      .exchange()
      .expectStatus().isOk
      .expectBody<List<String>>()
      .returnResult().responseBody

    assertThat(response!!.size).isEqualTo(1)
    assertThat(response[0]).isEqualTo("X123456")
  }

  companion object {
    private val data = TierCalculationResultEntity(
      protect = TierLevel(ProtectLevel.B, 4, mapOf(CalculationRule.ROSH to 4)),
      change = TierLevel(ChangeLevel.TWO, 12, mapOf(CalculationRule.COMPLEXITY to 12)),
      calculationVersion = "99",
    )
  }
}
