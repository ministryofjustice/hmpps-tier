package uk.gov.justice.digital.hmpps.hmppstier.service

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.hmppstier.client.Answer
import uk.gov.justice.digital.hmpps.hmppstier.client.AssessmentApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.client.Question
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AssessmentComplexityFactor
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@ExtendWith(MockKExtension::class)
@DisplayName("Detail Service tests")
internal class AssessmentApiServiceTest {
  private val assessmentApiClient: AssessmentApiClient = mockk(relaxUnitFun = true)
  private val clock = Clock.fixed(LocalDate.of(2021, 1, 20).atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())
  private val assessmentService = AssessmentApiService(assessmentApiClient, clock)

  @BeforeEach
  fun resetAllMocks() {
    clearMocks(assessmentApiClient)
  }

  @Nested
  @DisplayName("Get Complexity Answer Tests")
  inner class GetComplexityAnswerTests {

    @Test
    fun `Should return Complexity Answer if present and positive`() {
      val crn = "123"
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null)
      val complexityAnswers =
        listOf(
          Question(
            "13.3 - F",
            setOf(Answer("Yes"))
          )
        )

      every { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment)
      every {
        assessmentApiClient.getAssessmentAnswers(
          assessment.assessmentId
        )
      } returns complexityAnswers
      val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

      assertThat(returnValue).hasSize(1)
      assertThat(returnValue).containsKey(AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES)
    }

    @Test
    fun `Should return Complexity Answer even if present and negative`() {
      val crn = "123"
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null)
      val complexityAnswers =
        listOf(
          Question(
            "13.3 - F",
            setOf(Answer("No"))
          )
        )

      every { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment)
      every {
        assessmentApiClient.getAssessmentAnswers(
          assessment.assessmentId
        )
      } returns complexityAnswers
      val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

      assertThat(returnValue).hasSize(1)
      assertThat(returnValue).containsKey(AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES)
    }

    @Test
    fun `Should match Complexity Answer Case Insensitive Question`() {
      val crn = "123"
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null)
      val complexityAnswers =
        listOf(
          Question(
            "13.3 - f",
            setOf(Answer("Yes"))
          )
        )

      every { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment)
      every {
        assessmentApiClient.getAssessmentAnswers(
          assessment.assessmentId
        )
      } returns complexityAnswers
      val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

      assertThat(returnValue).hasSize(1)
      assertThat(returnValue).containsKey(AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES)
    }

    @Test
    fun `Should match Complexity Answer Case Insensitive Answer`() {
      val crn = "123"
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null)
      val complexityAnswers =
        listOf(
          Question(
            "13.3 - F",
            setOf(Answer("YeS"))
          )
        )

      every { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment)
      every {
        assessmentApiClient.getAssessmentAnswers(
          assessment.assessmentId
        )
      } returns complexityAnswers
      val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

      assertThat(returnValue).hasSize(1)
      assertThat(returnValue).containsKey(AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES)
    }

    @Test
    fun `Should return empty List if no Complexity Answers match`() {
      val crn = "123"
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null)
      val complexityAnswers =
        listOf(
          Question(
            "15.3",
            setOf(Answer("Yes"))
          )
        )

      every { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment)
      every {
        assessmentApiClient.getAssessmentAnswers(
          assessment.assessmentId
        )
      } returns complexityAnswers
      val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

      assertThat(returnValue).isEmpty()
    }

    @Test
    fun `Should return Complexity any Answers Match`() {
      val crn = "123"
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null)
      val complexityAnswers =
        listOf(
          Question(
            "13.3 - F",
            setOf(Answer("No"), Answer("No"), Answer("Yes"))
          )
        )

      every { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment)
      every {
        assessmentApiClient.getAssessmentAnswers(
          assessment.assessmentId
        )
      } returns complexityAnswers
      val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

      assertThat(returnValue).hasSize(1)
      assertThat(returnValue).containsKey(AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES)
    }

    @Test
    fun `Should return multiple Complexity Answers`() {
      val crn = "123"
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null)
      val complexityAnswers =
        listOf(
          Question(
            "13.3 - F",
            setOf(Answer("No"), Answer("No"), Answer("Yes"))
          ),
          Question(
            "11.4",
            setOf(Answer("Yes"))
          )
        )

      every { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment)
      every {
        assessmentApiClient.getAssessmentAnswers(
          assessment.assessmentId
        )
      } returns complexityAnswers
      val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

      assertThat(returnValue).hasSize(2)
      assertThat(returnValue).containsKey(AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES)
      assertThat(returnValue).containsKey(AssessmentComplexityFactor.TEMPER_CONTROL)
    }

    @Test
    fun `Should return empty List if no Complexity Answers present`() {
      val crn = "123"
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null)
      val complexityAnswers =
        listOf<Question>()

      every { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment)
      every {
        assessmentApiClient.getAssessmentAnswers(
          assessment.assessmentId
        )
      } returns complexityAnswers
      val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

      assertThat(returnValue).isEmpty()
    }
  }
}
