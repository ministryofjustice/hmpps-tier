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
      val convictions = listOf(Conviction(54321L, Sentence(null, KeyValue("SC"), LocalDate.now(clock), LocalDate.now(clock).plusDays(1), KeyValue("101")), listOf(offence)))

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
      val convictions = listOf(Conviction(54321L, Sentence(null, KeyValue("SC"), LocalDate.now(clock), LocalDate.now(clock).plusDays(1), KeyValue("101")), listOf(offence)))

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
      val convictions = listOf(Conviction(54321L, Sentence(null, KeyValue("SC"), LocalDate.now(clock), LocalDate.now(clock).plusDays(1), KeyValue("101")), listOf(offence)))

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
}
