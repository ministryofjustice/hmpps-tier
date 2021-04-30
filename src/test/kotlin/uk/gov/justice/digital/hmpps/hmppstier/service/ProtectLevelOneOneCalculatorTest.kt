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
import uk.gov.justice.digital.hmpps.hmppstier.client.KeyValue
import uk.gov.justice.digital.hmpps.hmppstier.client.Offence
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenceDetail
import uk.gov.justice.digital.hmpps.hmppstier.client.Offender
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.client.Sentence
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.OffenceCode
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@ExtendWith(MockKExtension::class)
@DisplayName("Protect Level Calculator tests")
internal class ProtectLevelOneOneCalculatorTest {

  private val clock = Clock.fixed(LocalDateTime.of(2020, 1, 1, 0, 0).toInstant(ZoneOffset.UTC), ZoneId.systemDefault())
  private val communityApiClient: CommunityApiClient = mockk(relaxUnitFun = true)
  private val assessmentApiService: AssessmentApiService = mockk(relaxUnitFun = true)

  private val service = ProtectLevelCalculator(
    clock,
    communityApiClient,
    assessmentApiService,
    1.1F
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
  @DisplayName("Arson and Violence tests")
  inner class ArsonViolenceTests {

    @Test
    fun `should count valid Offence code`() {
      val convictionId = 54321L

      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      val offence = Offence(OffenceDetail(OffenceCode._056.code))
      val convictions = listOf(Conviction(54321L, Sentence(null, KeyValue("SC"), LocalDate.now(clock), LocalDate.now(clock).plusDays(1)), listOf(offence), "101"))

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns listOf()
      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns mapOf()

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), convictions)
      assertThat(result.points).isEqualTo(2)

      verify { communityApiClient.getOffender(crn) }
      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should Not count valid Offence code Male`() {

      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      val offence = Offence(OffenceDetail(OffenceCode._056.code))
      val convictions = listOf(Conviction(54321L, Sentence(null, KeyValue("SC"), LocalDate.now(clock), LocalDate.now(clock).plusDays(1)), listOf(offence), "101"))

      every { communityApiClient.getOffender(crn) } returns Offender("Male")

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), convictions)
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getOffender(crn) }
    }

    @Test
    fun `should not count invalid Offence code`() {
      val convictionId = 54321L

      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      val offence = Offence(OffenceDetail("Any Invalid Code"))
      val convictions = listOf(Conviction(54321L, Sentence(null, KeyValue("SC"), LocalDate.now(clock), LocalDate.now(clock).plusDays(1)), listOf(offence), "101"))

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns listOf()
      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns mapOf()

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), convictions)
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getOffender(crn) }
      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }
  }

  @Nested
  @DisplayName("Sentence Length tests")
  inner class SentenceLengthTests {

    @Test
    fun `should count sentence longer than 10 months`() {

      val assessment = getValidAssessment()

      val convictionId = 54321L
      val sentence = Sentence(null, KeyValue("SC"), LocalDate.now(clock), LocalDate.now(clock).plusMonths(11))
      val convictions = getValidConviction(convictionId, sentence, "Not Indeterminate")

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns listOf()
      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns mapOf()

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), convictions)
      assertThat(result.points).isEqualTo(2)

      verify { communityApiClient.getOffender(crn) }
      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should count sentence exactly 10 months`() {

      val assessment = getValidAssessment()

      val convictionId = 54321L
      val sentence = Sentence(null, KeyValue("SC"), LocalDate.now(clock), LocalDate.now(clock).plusMonths(10))
      val convictions = getValidConviction(convictionId, sentence, "Not Indeterminate")

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns listOf()
      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns mapOf()

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), convictions)
      assertThat(result.points).isEqualTo(2)

      verify { communityApiClient.getOffender(crn) }
      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should not count sentence less than 10 months`() {

      val assessment = getValidAssessment()

      val convictionId = 54321L
      val sentence = Sentence(null, KeyValue("SC"), LocalDate.now(clock), LocalDate.now(clock).plusMonths(9))
      val convictions = getValidConviction(convictionId, sentence, "Not Indeterminate")

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns listOf()
      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns mapOf()

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), convictions)
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getOffender(crn) }
      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should not count sentence null startDate`() {

      val assessment = getValidAssessment()

      val convictionId = 54321L
      val sentence = Sentence(null, KeyValue("SC"), null, LocalDate.now(clock).plusMonths(10))
      val convictions = getValidConviction(convictionId, sentence, "Not Indeterminate")

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns listOf()
      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns mapOf()

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), convictions)
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getOffender(crn) }
      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should not count sentence null endDate`() {

      val assessment = getValidAssessment()

      val convictionId = 54321L
      val sentence = Sentence(null, KeyValue("SC"), LocalDate.now(), null)
      val convictions = getValidConviction(convictionId, sentence, "Not Indeterminate")

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns listOf()
      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns mapOf()

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), convictions)
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getOffender(crn) }
      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should not count invalid sentence code`() {

      val assessment = getValidAssessment()

      val convictionId = 54321L
      val sentence = Sentence(null, KeyValue("SC"), null, null)
      val convictions = getValidConviction(convictionId, sentence, "Not Indeterminate")

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns listOf()
      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns mapOf()

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), convictions)
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getOffender(crn) }
      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should count valid sentence code`() {

      val assessment = getValidAssessment()

      val convictionId = 54321L
      val sentence = Sentence(null, KeyValue("SC"), null, null)
      val convictions = getValidConviction(convictionId, sentence, "303")

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns listOf()
      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns mapOf()

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), convictions)
      assertThat(result.points).isEqualTo(2)

      verify { communityApiClient.getOffender(crn) }
      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should not count valid sentence code when male`() {

      val assessment = getValidAssessment()

      val convictionId = 54321L
      val sentence = Sentence(null, KeyValue("SC"), null, null)
      val convictions = getValidConviction(convictionId, sentence, "303")

      every { communityApiClient.getOffender(crn) } returns Offender("Male")
      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), convictions)
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getOffender(crn) }
    }

    @Test
    fun `should not count non-custodial sentence`() {

      val assessment = getValidAssessment()

      val convictionId = 54321L
      val sentence = Sentence(null, KeyValue("HG"), null, null)
      val convictions = getValidConviction(convictionId, sentence, "303")

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns listOf()
      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns mapOf()

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), convictions)
      assertThat(result.points).isEqualTo(0)

      verify { communityApiClient.getOffender(crn) }
      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should not double count sentence longer than 10 months and indeterminate`() {

      val assessment = getValidAssessment()

      val convictionId = 54321L
      val sentence = Sentence(null, KeyValue("SC"), LocalDate.now(clock), LocalDate.now(clock).plusMonths(11))
      val convictions = getValidConviction(convictionId, sentence, "303")

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns listOf()
      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns mapOf()

      val result = service.calculateProtectLevel(crn, assessment, null, listOf(), convictions)
      assertThat(result.points).isEqualTo(2) // not 4

      verify { communityApiClient.getOffender(crn) }
      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    private fun getValidAssessment() = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

    private fun getValidConviction(convictionId: Long, sentence: Sentence, courtAppearanceOutcome: String) = listOf(
      Conviction(
        convictionId, sentence, listOf(),
        courtAppearanceOutcome
      )
    )
  }
}
