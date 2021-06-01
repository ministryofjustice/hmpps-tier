package uk.gov.justice.digital.hmpps.hmppstier.service

import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.KeyValue
import uk.gov.justice.digital.hmpps.hmppstier.client.Nsi
import uk.gov.justice.digital.hmpps.hmppstier.client.Offender
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.domain.Sentence
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.RsrThresholds.TIER_C_RSR_LOWER
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@ExtendWith(MockKExtension::class)
@DisplayName("Protect Level Calculator tests")
internal class ProtectLevelCalculatorTest {

  private val clock = Clock.fixed(LocalDateTime.of(2020, 1, 1, 0, 0).toInstant(ZoneOffset.UTC), ZoneId.systemDefault())
  private val communityApiClient: CommunityApiClient = mockk(relaxUnitFun = true)
  private val assessmentApiService: AssessmentApiService = mockk(relaxUnitFun = true)
  private val additionalFactorsForWomen: AdditionalFactorsForWomen = AdditionalFactorsForWomen(clock, communityApiClient, assessmentApiService)
  private val service = ProtectLevelCalculator()

  private val crn = "Any Crn"

  private fun calculateProtectLevel(crn: String, offenderAssessment: OffenderAssessment? = null, rsr: BigDecimal = BigDecimal.ZERO, rosh: Rosh? = null, convictions: Collection<Conviction> = listOf()): TierLevel<ProtectLevel> {
    return service.calculateProtectLevel(
      rsr,
      rosh,
      null,
      listOf(),
      additionalFactorsForWomen.calculate(crn, convictions, offenderAssessment)
    )
  }

  @BeforeEach
  fun resetAllMocks() {
    clearMocks(communityApiClient)
    clearMocks(assessmentApiService)
  }

  @AfterEach
  fun confirmVerified() {
    // Check we don't add any more calls without updating the tests
    confirmVerified(communityApiClient)
    confirmVerified(assessmentApiService)
  }

  @Nested
  @DisplayName("Simple Risk tests")
  inner class SimpleRiskTests {

    @Test
    fun `should use either when RSR is same as ROSH`() {
      setup()
      // rsr C+1 = 10 points, Rosh.Medium = 10 Points
      val result = calculateProtectLevel(crn = crn, rsr = TIER_C_RSR_LOWER.num, rosh = Rosh.MEDIUM)
      assertThat(result.points).isEqualTo(10)
      validate()
    }

    private fun setup() {
      every { communityApiClient.getOffender(crn) } returns Offender("Female")
    }

    private fun validate() {
      verify { communityApiClient.getOffender(crn) }
    }
  }

  @Nested
  @DisplayName("Simple RSR tests")
  inner class SimpleRSRTests {

    @Test
    fun `should return 0 for RSR null`() {
      setup()
      val result = calculateProtectLevel(crn = crn, rsr = BigDecimal.ZERO)
      assertThat(result.points).isEqualTo(0)
      validate()
    }

    @Test
    fun `Should return RSR`() {
      setup()
      val result = calculateProtectLevel(crn = crn, rsr = BigDecimal(5))
      assertThat(result.points).isEqualTo(10)
      validate()
    }

    private fun setup() {
      every { communityApiClient.getOffender(crn) } returns Offender("Female")
    }

    private fun validate() {
      verify { communityApiClient.getOffender(crn) }
    }
  }

  @Nested
  @DisplayName("Get Breach Recall Tests")
  inner class GetBreachRecallTests {
    private val irrelevantSentenceType = "Irrelevant"

    @Test
    fun `Should return Breach true if present and valid terminationDate after cutoff`() {
      val crn = "123"
      val convictionId = 54321L
      val terminationDate = LocalDate.now(clock).minusYears(1)
      val sentence = Sentence(terminationDate, irrelevantSentenceType)
      val conviction = Conviction(convictionId, sentence)

      val breaches = listOf(Nsi(status = KeyValue("BRE08")))

      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches
      every { communityApiClient.getOffender(crn) } returns Offender("Female")

      val result = calculateProtectLevel(crn = crn, convictions = listOf(conviction))
      assertThat(result.points).isEqualTo(2)

      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { communityApiClient.getOffender(crn) }
    }

    @Test
    fun `Should return Breach false if present and valid terminationDate on cutoff`() {
      val crn = "123"
      val convictionId = 54321L
      val terminationDate = LocalDate.now(clock).minusYears(1).minusDays(1)
      val sentence = Sentence(terminationDate, irrelevantSentenceType)
      val conviction = Conviction(convictionId, sentence)

      every { communityApiClient.getOffender(crn) } returns Offender("Female")

      val result = calculateProtectLevel(crn = crn, convictions = listOf(conviction))
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getOffender(crn) }
    }

    @Test
    fun `Should return Breach true if present and valid not terminated`() {
      val crn = "123"
      val convictionId = 54321L
      val sentence = Sentence(null, irrelevantSentenceType)
      val conviction = Conviction(convictionId, sentence)

      val breaches = listOf(Nsi(status = KeyValue("BRE08")))

      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches
      every { communityApiClient.getOffender(crn) } returns Offender("Female")

      val result = calculateProtectLevel(crn = crn, convictions = listOf(conviction))
      assertThat(result.points).isEqualTo(2)

      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { communityApiClient.getOffender(crn) }
    }

    @Test
    fun `Should return Breach true if multiple convictions, one valid`() {
      val crn = "123"
      val convictionId = 54321L
      val sentence = Sentence(null, irrelevantSentenceType)
      val conviction = Conviction(convictionId, sentence)

      val unrelatedConviction = Conviction(convictionId.plus(1), sentence)
      val unrelatedBreaches = listOf(Nsi(status = KeyValue("BRE99")))

      val breaches = listOf(Nsi(status = KeyValue("BRE08")))

      every { communityApiClient.getBreachRecallNsis(crn, convictionId.plus(1)) } returns unrelatedBreaches
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches
      every { communityApiClient.getOffender(crn) } returns Offender("Female")

      val result = calculateProtectLevel(crn = crn, convictions = listOf(conviction, unrelatedConviction))
      assertThat(result.points).isEqualTo(2)

      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { communityApiClient.getOffender(crn) }
    }

    @Test
    fun `Should return Breach false if no conviction`() {
      val crn = "123"

      every { communityApiClient.getOffender(crn) } returns Offender("Female")

      val result = calculateProtectLevel(crn)
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getOffender(crn) }
    }

    @Test
    fun `Should return Breach true if one conviction, multiple breaches, one valid`() {
      val crn = "123"
      val convictionId = 54321L
      val sentence = Sentence(null, irrelevantSentenceType)
      val conviction = Conviction(convictionId, sentence)

      val breaches = listOf(
        Nsi(status = KeyValue("BRE54")),
        Nsi(status = KeyValue("BRE08"))
      )

      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches
      every { communityApiClient.getOffender(crn) } returns Offender("Female")

      val result = calculateProtectLevel(crn = crn, convictions = listOf(conviction))
      assertThat(result.points).isEqualTo(2)

      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { communityApiClient.getOffender(crn) }
    }

    @Test
    fun `Should return Breach true if one conviction, multiple breaches, one valid case insensitive`() {
      val crn = "123"
      val convictionId = 54321L
      val sentence = Sentence(null, irrelevantSentenceType)
      val conviction = Conviction(convictionId, sentence)

      val breaches = listOf(
        Nsi(status = KeyValue("BRE54")),
        Nsi(status = KeyValue("bre08"))
      )

      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches
      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      val result = calculateProtectLevel(crn = crn, convictions = listOf(conviction))

      assertThat(result.points).isEqualTo(2)

      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { communityApiClient.getOffender(crn) }
    }

    @Test
    fun `Should return Breach false if one conviction, multiple breaches, none valid`() {
      val crn = "123"
      val convictionId = 54321L
      val sentence = Sentence(null, irrelevantSentenceType)
      val conviction = Conviction(convictionId, sentence)

      val breaches = listOf(
        Nsi(status = KeyValue("BRE99")),
        Nsi(status = KeyValue("BRE99"))
      )

      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches
      every { communityApiClient.getOffender(crn) } returns Offender("Female")

      val result = calculateProtectLevel(crn = crn, convictions = listOf(conviction))
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { communityApiClient.getOffender(crn) }
    }
  }
}
