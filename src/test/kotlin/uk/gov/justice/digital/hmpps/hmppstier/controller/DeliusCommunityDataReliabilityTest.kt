package uk.gov.justice.digital.hmpps.hmppstier.controller

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.CalculationRule
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
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

  @Test
  fun `delius assessments data are identical per crn`() {
    setupCommunityApiAssessment(crn1)
    setupTierToDeliusFull(crn1)

    webTestClient.get()
      .uri("/crn/$crn1")
      .headers { it.authToken(roles = listOf("ROLE_HMPPS_TIER")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.crn")
      .isEqualTo(crn1)
      .jsonPath("$.rsrMatch")
      .isEqualTo("true")
      .jsonPath("$.ogrsMatch")
      .isEqualTo("true")
      .jsonPath("$.rsrDelius")
      .isEqualTo(23)
      .jsonPath("$.rsrCommunity")
      .isEqualTo(23)
      .jsonPath("$.ogrsDelius")
      .isEqualTo(2)
      .jsonPath("$.ogrsCommunity")
      .isEqualTo(2)
  }

  @Test
  fun `no delius assessments data`() {
    setupNoDeliusAssessment(crn1)
    setupTierToDeliusNoAssessment(crn1)
    webTestClient.get()
      .uri("/crn/$crn1")
      .headers { it.authToken(roles = listOf("ROLE_HMPPS_TIER")) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.crn")
      .isEqualTo(crn1)
      .jsonPath("$.rsrMatch")
      .isEqualTo("true")
      .jsonPath("$.ogrsMatch")
      .isEqualTo("true")
      .jsonPath("$.rsrDelius")
      .isEqualTo(0)
      .jsonPath("$.rsrCommunity")
      .isEqualTo(0)
      .jsonPath("$.ogrsDelius")
      .isEqualTo(0)
      .jsonPath("$.ogrsCommunity")
      .isEqualTo(0)
  }

  @Test
  fun `no delius currentTier data`() {
    setupNoDeliusAssessment(crn1)
    setupTierToDeliusNoTierResponse(crn1)
    webTestClient.get()
      .uri("/crn/$crn1")
      .headers { it.authToken(roles = listOf("ROLE_HMPPS_TIER")) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.crn")
      .isEqualTo(crn1)
  }

  @Test
  fun `delius and community assessments do not match`() {
    setupCommunityApiAssessment(crn1)
    setupTierToDeliusNoAssessment(crn1)
    webTestClient.get()
      .uri("/crn/$crn1")
      .headers { it.authToken(roles = listOf("ROLE_HMPPS_TIER")) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.crn")
      .isEqualTo(crn1)
      .jsonPath("$.rsrMatch")
      .isEqualTo("false")
      .jsonPath("$.ogrsMatch")
      .isEqualTo("false")
      .jsonPath("$.rsrDelius")
      .isEqualTo(0)
      .jsonPath("$.rsrCommunity")
      .isEqualTo(23)
      .jsonPath("$.ogrsDelius")
      .isEqualTo(0)
      .jsonPath("$.ogrsCommunity")
      .isEqualTo(2)
  }

  @Test
  fun `Should return reliability for all distinct crns`() {
    val firstTierCalculation = TierCalculationEntity(crn = crn1, created = created, data = data, uuid = UUID.randomUUID())
    val secondTierCalculation = TierCalculationEntity(crn = crn2, created = created.minusHours(1), data = data, uuid = UUID.randomUUID())
    val thirdTierCalculation = TierCalculationEntity(crn = crn2, created = created.minusDays(1), data = data, uuid = UUID.randomUUID())
    repository.saveAll(listOf(firstTierCalculation, secondTierCalculation, thirdTierCalculation))

    setupCommunityApiAssessment(crn1)
    setupCommunityApiAssessment(crn2)
    setupTierToDeliusFull(crn1)
    setupTierToDeliusFull(crn2)

    webTestClient.get()
      .uri("/crn/all")
      .headers { it.authToken(roles = listOf("ROLE_HMPPS_TIER")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.length()")
      .isEqualTo(2)
      .jsonPath("$.[0].crn")
      .isEqualTo(crn1)
      .jsonPath("$.[1].crn")
      .isEqualTo(crn2)
  }

  @Test
  fun `Should return failed reliability for all distinct crns`() {
    val firstTierCalculation = TierCalculationEntity(crn = crn1, created = created, data = data, uuid = UUID.randomUUID())
    val secondTierCalculation = TierCalculationEntity(crn = crn2, created = created, data = data, uuid = UUID.randomUUID())
    repository.saveAll(listOf(firstTierCalculation, secondTierCalculation))
    setupCommunityApiAssessment(crn1)
    setupCommunityApiAssessment(crn2)
    setupTierToDeliusFull(crn1)
    setupTierToDeliusFull(crn2, ogrsscore = "0", rsrscore = "0")
    webTestClient.get().uri("/crn/all").headers { it.authToken(roles = listOf("ROLE_HMPPS_TIER")) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.[0].crn")
      .isEqualTo(crn1)
      .jsonPath("$.[0].rsrMatch")
      .isEqualTo("true")
      .jsonPath("$.[0].ogrsMatch")
      .isEqualTo("true")
      .jsonPath("$.[1].crn")
      .isEqualTo(crn2)
      .jsonPath("$.[1].rsrMatch")
      .isEqualTo("false")
      .jsonPath("$.[1].ogrsMatch")
      .isEqualTo("false")
      .jsonPath("$.[1].rsrDelius")
      .isEqualTo(0)
      .jsonPath("$.[1].ogrsDelius")
      .isEqualTo(0)
  }

  @Test
  fun `Tier to Delius Not found`() {
    val firstTierCalculation = TierCalculationEntity(crn = crn1, created = created, data = data, uuid = UUID.randomUUID())
    val secondTierCalculation = TierCalculationEntity(crn = crn2, created = created, data = data, uuid = UUID.randomUUID())
    repository.saveAll(listOf(firstTierCalculation, secondTierCalculation))
    setupCommunityApiAssessment(crn1)
    setupCommunityApiAssessment(crn2)
    setupTierToDeliusFull(crn1)
    setupTierToDeliusNotFound(crn2)
    webTestClient.get().uri("/crn/all").headers { it.authToken(roles = listOf("ROLE_HMPPS_TIER")) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.[0].crn")
      .isEqualTo(crn1)
      .jsonPath("$.[0].rsrMatch")
      .isEqualTo("true")
      .jsonPath("$.[0].ogrsMatch")
      .isEqualTo("true")
      .jsonPath("$.[1].crn")
      .isEqualTo(crn2)
      .jsonPath("$.[1].rsrMatch")
      .isEqualTo("false")
      .jsonPath("$.[1].ogrsMatch")
      .isEqualTo("false")
      .jsonPath("$.[1].rsrDelius")
      .isEqualTo(-1)
      .jsonPath("$.[1].ogrsDelius")
      .isEqualTo(-1)
  }

  companion object {
    private val data = TierCalculationResultEntity(
      protect = TierLevel(ProtectLevel.B, 4, mapOf(CalculationRule.ROSH to 4)),
      change = TierLevel(ChangeLevel.TWO, 12, mapOf(CalculationRule.COMPLEXITY to 12)),
      calculationVersion = "99",
    )
  }
}
