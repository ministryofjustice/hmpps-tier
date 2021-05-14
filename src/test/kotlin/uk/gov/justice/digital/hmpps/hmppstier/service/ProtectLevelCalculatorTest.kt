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
import uk.gov.justice.digital.hmpps.hmppstier.client.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusAssessments
import uk.gov.justice.digital.hmpps.hmppstier.client.KeyValue
import uk.gov.justice.digital.hmpps.hmppstier.client.Nsi
import uk.gov.justice.digital.hmpps.hmppstier.client.Offence
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenceDetail
import uk.gov.justice.digital.hmpps.hmppstier.client.Offender
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.client.Registration
import uk.gov.justice.digital.hmpps.hmppstier.client.Sentence
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.OffenceCode
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.RsrThresholds
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

  private val service = ProtectLevelCalculator(
    clock,
    communityApiClient,
    assessmentApiService,
    1
  )

  private val crn = "Any Crn"

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
      val result = service.calculateProtectLevel(crn, null, getValidAssessments(TIER_C_RSR_LOWER), getValidRegistrations(Rosh.MEDIUM), listOf())
      assertThat(result.points).isEqualTo(10)
      validate()
    }

    private fun getValidAssessments(rsr: RsrThresholds): DeliusAssessments {
      return DeliusAssessments(
        rsr = rsr.num.plus(BigDecimal(1)),
        ogrs = null
      )
    }

    private fun getValidRegistrations(rosh: Rosh): Collection<Registration> {
      return listOf(Registration(type = KeyValue(rosh.registerCode), registerLevel = null, startDate = LocalDate.now(clock)))
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
      val result = service.calculateProtectLevel(crn, null, getValidAssessments(), listOf(), listOf())
      assertThat(result.points).isEqualTo(0)
      validate()
    }

    @Test
    fun `should return 0 for RSR null no assessment object`() {
      setup()
      val nulLDeliusAssessments: DeliusAssessments? = null
      val result = service.calculateProtectLevel(crn, null, nulLDeliusAssessments, listOf(), listOf())
      assertThat(result.points).isEqualTo(0)
      validate()
    }

    @Test
    fun `Should return RSR`() {
      val assessment = DeliusAssessments(
        rsr = BigDecimal(5),
        ogrs = null,
      )

      setup()
      val result = service.calculateProtectLevel(crn, null, assessment, listOf(), listOf())
      assertThat(result.points).isEqualTo(10)
      validate()
    }

    private fun getValidAssessments(): DeliusAssessments {
      return DeliusAssessments(
        rsr = null,
        ogrs = null
      )
    }

    private fun setup() {
      every { communityApiClient.getOffender(crn) } returns Offender("Female")
    }

    private fun validate() {
      verify { communityApiClient.getOffender(crn) }
    }
  }

  @Nested
  @DisplayName("Simple ROSH tests")
  inner class SimpleRoshTests {

    @Test
    fun `should return 0 for No Rosh`() {
      setup()
      val result = service.calculateProtectLevel(crn, null, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(0)
      validate()
    }

    @Test
    fun `Should return RoSH level if present`() {
      setup()

      val registrations =
        listOf(
          Registration(
            KeyValue("RMRH"),
            KeyValue("Not Used"),

            LocalDate.now()
          )
        )

      val result = service.calculateProtectLevel(crn, null, null, registrations, listOf())

      assertThat(result.points).isEqualTo(10)

      validate()
    }

    @Test
    fun `Should return RoSH level Case Insensitive`() {
      setup()

      val registrations =
        listOf(
          Registration(
            KeyValue("rmrh"),
            KeyValue("Not Used"),

            LocalDate.now()
          )
        )

      val result = service.calculateProtectLevel(crn, null, null, registrations, listOf())

      assertThat(result.points).isEqualTo(10)

      validate()
    }

    @Test
    fun `Should return RoSH level if present as first value in list`() {
      setup()

      val registrations =
        listOf(
          Registration(
            KeyValue("RMRH"),
            KeyValue("Not Used"),

            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S"),
            KeyValue("Not Used"),

            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S"),
            KeyValue("Not Used"),

            LocalDate.now()
          ),
        )

      val result = service.calculateProtectLevel(crn, null, null, registrations, listOf())

      assertThat(result.points).isEqualTo(10)

      validate()
    }

    @Test
    fun `Should return RoSH level if present as middle value in list`() {
      setup()

      val registrations =
        listOf(
          Registration(
            KeyValue("AV2S"),
            KeyValue("Not Used"),

            LocalDate.now()
          ),
          Registration(
            KeyValue("RMRH"),
            KeyValue("Not Used"),

            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S"),
            KeyValue("Not Used"),

            LocalDate.now()
          ),

        )

      val result = service.calculateProtectLevel(crn, null, null, registrations, listOf())

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
  @DisplayName("Simple Mappa tests")
  inner class SimpleMappaTests {

    @Test
    fun `should return 0 for Mappa null`() {
      setup()
      val result = service.calculateProtectLevel(crn, null, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(0)
      validate()
    }

    @Test
    fun `Should return Mappa level if present`() {
      val registrations =
        listOf(
          Registration(
            KeyValue("Not Used"),
            KeyValue("M3"),

            LocalDate.now()
          )
        )

      setup()
      val result = service.calculateProtectLevel(crn, null, null, registrations, listOf())
      validate()

      assertThat(result.points).isEqualTo(30)
    }

    @Test
    fun `Should return Mappa level Case Insensitive`() {
      val registrations =
        listOf(
          Registration(
            KeyValue("Not Used"),
            KeyValue("m3"),

            LocalDate.now()
          )
        )

      setup()
      val result = service.calculateProtectLevel(crn, null, null, registrations, listOf())
      validate()

      assertThat(result.points).isEqualTo(30)
    }

    @Test
    fun `Should return Mappa level if present as first value in list`() {
      val registrations =
        listOf(
          Registration(
            KeyValue("Not Used"),
            KeyValue("M3"),

            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S"),
            KeyValue("BD"),

            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S"),
            KeyValue("12"),

            LocalDate.now()
          ),

        )

      setup()
      val result = service.calculateProtectLevel(crn, null, null, registrations, listOf())
      validate()

      assertThat(result.points).isEqualTo(30)
    }

    @Test
    fun `Should return null for invalid Mappa code`() {
      val registrations =
        listOf(
          Registration(
            KeyValue("AV2S"),
            KeyValue("Not Used"),

            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S"),
            KeyValue("Not Used"),

            LocalDate.now()
          ),
          Registration(
            KeyValue("Not Used"),
            KeyValue("INVALID"),

            LocalDate.now()
          ),
        )

      setup()
      val result = service.calculateProtectLevel(crn, null, null, registrations, listOf())
      validate()

      assertThat(result.points).isEqualTo(0)
    }

    private fun setup() {
      every { communityApiClient.getOffender(crn) } returns Offender("Female")
    }

    private fun validate() {
      verify { communityApiClient.getOffender(crn) }
    }
  }

  @Nested
  @DisplayName("Simple Complexity tests")
  inner class SimpleComplexityTests {

    @Test
    fun `should count complexity factors `() {
      setup()
      val result = service.calculateProtectLevel(
        crn, null, null,
        getValidRegistrations(
          listOf(
            ComplexityFactor.VULNERABILITY_ISSUE,
            ComplexityFactor.ADULT_AT_RISK,

          )
        ),
        listOf()
      )
      assertThat(result.points).isEqualTo(4)
      validate()
    }

    @Test
    fun `should not count complexity factors duplicates`() {
      setup()
      val result = service.calculateProtectLevel(
        crn, null, null,
        getValidRegistrations(
          listOf(
            ComplexityFactor.VULNERABILITY_ISSUE,
            ComplexityFactor.VULNERABILITY_ISSUE,
          )
        ),
        listOf()
      )
      assertThat(result.points).isEqualTo(2)
      validate()
    }

    @Test
    fun `should not count complexity factors none`() {
      setup()
      val result = service.calculateProtectLevel(crn, null, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(0)
      validate()
    }

    @Test
    fun `Should return Complexity Factor if present`() {
      val registrations =
        listOf(
          Registration(
            KeyValue("RMDO"),
            KeyValue("Not Used"),

            LocalDate.now()
          )
        )

      setup()
      val result = service.calculateProtectLevel(crn, null, null, registrations, listOf())
      validate()

      assertThat(result.points).isEqualTo(2)
    }

    @Test
    fun `Should return Complexity Factor Case Insensitive`() {
      val registrations =
        listOf(
          Registration(
            KeyValue("rmdo"),
            KeyValue("Not Used"),

            LocalDate.now()
          )
        )

      setup()
      val result = service.calculateProtectLevel(crn, null, null, registrations, listOf())
      validate()

      assertThat(result.points).isEqualTo(2)
    }

    @Test
    fun `Should return Complexity Factor if present as first value in list`() {
      val registrations =
        listOf(
          Registration(
            KeyValue("RMDO"),
            KeyValue("Not Used"),

            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S"),
            KeyValue("BD"),

            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S"),
            KeyValue("12"),

            LocalDate.now()
          ),

        )

      setup()
      val result = service.calculateProtectLevel(crn, null, null, registrations, listOf())
      validate()

      assertThat(result.points).isEqualTo(2)
    }

    @Test
    fun `Should return empty List if no Complexity Factors present`() {

      val registrations =
        listOf(
          Registration(
            KeyValue("AV2S"),
            KeyValue("Not Used"),

            LocalDate.now()
          )
        )

      setup()
      val result = service.calculateProtectLevel(crn, null, null, registrations, listOf())
      validate()

      assertThat(result.points).isEqualTo(0)
    }

    private fun getValidRegistrations(factors: List<ComplexityFactor>): Collection<Registration> {
      return factors.map {
        Registration(type = KeyValue(it.registerCode), registerLevel = null, startDate = LocalDate.now(clock))
      }
    }

    private fun setup() {
      every { communityApiClient.getOffender(crn) } returns Offender("Female")
    }

    private fun validate() {
      verify { communityApiClient.getOffender(crn) }
    }
  }

  @Nested
  @DisplayName("Additional Factors For Women tests")
  inner class AdditionalFactorsForWomenTests {
    @Test
    fun `should not count assessment additional factors duplicates`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
          AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
          AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
          AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
          AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
          AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
        )

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(2)

      verify { communityApiClient.getOffender(crn) }
      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should add multiple additional factors`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
          AdditionalFactorForWomen.TEMPER_CONTROL to "1",
        )

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(4)

      verify { communityApiClient.getOffender(crn) }
      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should not include additional factors if no valid assessment`() {
      val assessment = null

      every { communityApiClient.getOffender(crn) } returns Offender("Female")

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getOffender(crn) }
    }

    @Test
    fun `should count both Temper and Impulsivity as max '1'`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AdditionalFactorForWomen.IMPULSIVITY to "2",
          AdditionalFactorForWomen.TEMPER_CONTROL to "1",
        )

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(2) // 1 * 2 weighting for all additional factors

      verify { communityApiClient.getOffender(crn) }
      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should count Temper without Impulsivity as max '2'`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AdditionalFactorForWomen.TEMPER_CONTROL to "1",
        )

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(2) // 1 * 2 weighting for all additional factors

      verify { communityApiClient.getOffender(crn) }
      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should count Impulsivity without Temper as max '1'`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AdditionalFactorForWomen.IMPULSIVITY to "2",
        )

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(2) // 1 * 2 weighting for all additional factors

      verify { communityApiClient.getOffender(crn) }
      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should ignore negative Parenting`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "N",
        )

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getOffender(crn) }
      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should ignore negative Impulsivity`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AdditionalFactorForWomen.IMPULSIVITY to "0",
        )

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getOffender(crn) }
      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should ignore negative Temper`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AdditionalFactorForWomen.TEMPER_CONTROL to "0",
        )

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getOffender(crn) }
      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }
  }

  @Nested
  @DisplayName("Get Breach Recall Tests")
  inner class GetBreachRecallTests {
    private val irrelevantSentenceType: KeyValue = KeyValue("irrelevant")

    @Test
    fun `Should return Breach true if present and valid terminationDate after cutoff`() {
      val crn = "123"
      val convictionId = 54321L
      val terminationDate = LocalDate.now(clock).minusYears(1)
      val sentence = Sentence(terminationDate, irrelevantSentenceType, LocalDate.now(clock), LocalDate.now(clock).plusDays(1))
      val conviction = Conviction(convictionId, sentence, listOf(), "101")

      val breaches = listOf(Nsi(status = KeyValue("BRE08")))

      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches
      every { communityApiClient.getOffender(crn) } returns Offender("Female")

      val result = service.calculateProtectLevel(crn, null, null, listOf(), listOf(conviction))
      assertThat(result.points).isEqualTo(2)

      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { communityApiClient.getOffender(crn) }
    }

    @Test
    fun `Should return Breach false if present and valid terminationDate on cutoff`() {
      val crn = "123"
      val convictionId = 54321L
      val terminationDate = LocalDate.now(clock).minusYears(1).minusDays(1)
      val sentence = Sentence(terminationDate, irrelevantSentenceType, LocalDate.now(clock), LocalDate.now(clock).plusDays(1))
      val conviction = Conviction(convictionId, sentence, listOf(), "101")

      every { communityApiClient.getOffender(crn) } returns Offender("Female")

      val result = service.calculateProtectLevel(crn, null, null, listOf(), listOf(conviction))
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getOffender(crn) }
    }

    @Test
    fun `Should return Breach true if present and valid not terminated`() {
      val crn = "123"
      val convictionId = 54321L
      val sentence = Sentence(null, irrelevantSentenceType, LocalDate.now(clock), LocalDate.now(clock).plusDays(1))
      val conviction = Conviction(convictionId, sentence, listOf(), "101")

      val breaches = listOf(Nsi(status = KeyValue("BRE08")))

      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches
      every { communityApiClient.getOffender(crn) } returns Offender("Female")

      val result = service.calculateProtectLevel(crn, null, null, listOf(), listOf(conviction))
      assertThat(result.points).isEqualTo(2)

      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { communityApiClient.getOffender(crn) }
    }

    @Test
    fun `Should return Breach true if multiple convictions, one valid`() {
      val crn = "123"
      val convictionId = 54321L
      val sentence = Sentence(null, irrelevantSentenceType, LocalDate.now(clock), LocalDate.now(clock).plusDays(1))
      val conviction = Conviction(convictionId, sentence, listOf(), "101")

      val unrelatedConviction = Conviction(convictionId.plus(1), sentence, listOf(), "101")
      val unrelatedBreaches = listOf(Nsi(status = KeyValue("BRE99")))

      val breaches = listOf(Nsi(status = KeyValue("BRE08")))

      every { communityApiClient.getBreachRecallNsis(crn, convictionId.plus(1)) } returns unrelatedBreaches
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches
      every { communityApiClient.getOffender(crn) } returns Offender("Female")

      val result = service.calculateProtectLevel(crn, null, null, listOf(), listOf(conviction, unrelatedConviction))
      assertThat(result.points).isEqualTo(2)

      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { communityApiClient.getOffender(crn) }
    }

    @Test
    fun `Should return Breach false if no conviction`() {
      val crn = "123"

      every { communityApiClient.getOffender(crn) } returns Offender("Female")

      val result = service.calculateProtectLevel(crn, null, null, listOf(), listOf())
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getOffender(crn) }
    }

    @Test
    fun `Should return Breach true if one conviction, multiple breaches, one valid`() {
      val crn = "123"
      val convictionId = 54321L
      val sentence = Sentence(null, irrelevantSentenceType, LocalDate.now(clock), LocalDate.now(clock).plusDays(1))
      val conviction = Conviction(convictionId, sentence, listOf(), "101")

      val breaches = listOf(
        Nsi(status = KeyValue("BRE54")),
        Nsi(status = KeyValue("BRE08"))
      )

      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches
      every { communityApiClient.getOffender(crn) } returns Offender("Female")

      val result = service.calculateProtectLevel(crn, null, null, listOf(), listOf(conviction))
      assertThat(result.points).isEqualTo(2)

      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { communityApiClient.getOffender(crn) }
    }

    @Test
    fun `Should return Breach true if one conviction, multiple breaches, one valid case insensitive`() {
      val crn = "123"
      val convictionId = 54321L
      val sentence = Sentence(null, irrelevantSentenceType, LocalDate.now(clock), LocalDate.now(clock).plusDays(1))
      val conviction = Conviction(convictionId, sentence, listOf(), "101")

      val breaches = listOf(
        Nsi(status = KeyValue("BRE54")),
        Nsi(status = KeyValue("bre08"))
      )

      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches
      every { communityApiClient.getOffender(crn) } returns Offender("Female")

      val result = service.calculateProtectLevel(crn, null, null, listOf(), listOf(conviction))
      assertThat(result.points).isEqualTo(2)

      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { communityApiClient.getOffender(crn) }
    }

    @Test
    fun `Should return Breach false if one conviction, multiple breaches, none valid`() {
      val crn = "123"
      val convictionId = 54321L
      val sentence = Sentence(null, irrelevantSentenceType, LocalDate.now(clock), LocalDate.now(clock).plusDays(1))
      val conviction = Conviction(convictionId, sentence, listOf(), "101")

      val breaches = listOf(
        Nsi(status = KeyValue("BRE99")),
        Nsi(status = KeyValue("BRE99"))
      )

      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches
      every { communityApiClient.getOffender(crn) } returns Offender("Female")

      val result = service.calculateProtectLevel(crn, null, null, listOf(), listOf(conviction))
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { communityApiClient.getOffender(crn) }
    }
  }

  @Nested
  @DisplayName("Arson and Violence tests")
  inner class ArsonViolenceTests {
    @Test
    fun `Should respect Arson & Violence Toggle`() {
      val convictionId = 54321L

      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns listOf()
      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns mapOf()

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), getValidConviction())
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getOffender(crn) }
      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    private fun getValidConviction(): List<Conviction> {
      val offence = Offence(OffenceDetail(OffenceCode._056.code))
      return listOf(Conviction(54321L, Sentence(null, KeyValue("SC"), LocalDate.now(clock), LocalDate.now(clock).plusDays(1)), listOf(offence), "101"))
    }
  }
}
