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
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.hmppstier.client.Answer
import uk.gov.justice.digital.hmpps.hmppstier.client.AssessmentApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.AssessmentNeed
import uk.gov.justice.digital.hmpps.hmppstier.client.Question
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AssessmentComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import uk.gov.justice.digital.hmpps.hmppstier.service.exception.EntityNotFoundException

@ExtendWith(MockKExtension::class)
@DisplayName("Detail Service tests")
internal class AssessmentApiDataServiceTest {
  private val assessmentApiClient: AssessmentApiClient = mockk(relaxUnitFun = true)
  private val assessmentService = AssessmentApiDataService(assessmentApiClient)

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
      val assessmentId = "1234"
      val complexityAnswers =
        listOf(
          Question(
            "13.3 - F",
            setOf(Answer("Yes"))
          )
        )

      every { assessmentApiClient.getLatestAssessmentId(crn) } returns assessmentId
      every {
        assessmentApiClient.getAssessmentAnswers(
          assessmentId
        )
      } returns complexityAnswers
      val returnValue = assessmentService.getAssessmentComplexityAnswers(crn)

      assertThat(returnValue).hasSize(1)
      assertThat(returnValue).containsKey(AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES)
    }

    @Test
    fun `Should return Complexity Answer even if present and negative`() {
      val crn = "123"
      val assessmentId = "1234"
      val complexityAnswers =
        listOf(
          Question(
            "13.3 - F",
            setOf(Answer("No"))
          )
        )

      every { assessmentApiClient.getLatestAssessmentId(crn) } returns assessmentId
      every {
        assessmentApiClient.getAssessmentAnswers(
          assessmentId
        )
      } returns complexityAnswers
      val returnValue = assessmentService.getAssessmentComplexityAnswers(crn)

      assertThat(returnValue).hasSize(1)
      assertThat(returnValue).containsKey(AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES)
    }

    @Test
    fun `Should match Complexity Answer Case Insensitive Question`() {
      val crn = "123"
      val assessmentId = "1234"
      val complexityAnswers =
        listOf(
          Question(
            "13.3 - f",
            setOf(Answer("Yes"))
          )
        )

      every { assessmentApiClient.getLatestAssessmentId(crn) } returns assessmentId
      every {
        assessmentApiClient.getAssessmentAnswers(
          assessmentId
        )
      } returns complexityAnswers
      val returnValue = assessmentService.getAssessmentComplexityAnswers(crn)

      assertThat(returnValue).hasSize(1)
      assertThat(returnValue).containsKey(AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES)
    }

    @Test
    fun `Should match Complexity Answer Case Insensitive Answer`() {
      val crn = "123"
      val assessmentId = "1234"
      val complexityAnswers =
        listOf(
          Question(
            "13.3 - F",
            setOf(Answer("YeS"))
          )
        )

      every { assessmentApiClient.getLatestAssessmentId(crn) } returns assessmentId
      every {
        assessmentApiClient.getAssessmentAnswers(
          assessmentId
        )
      } returns complexityAnswers
      val returnValue = assessmentService.getAssessmentComplexityAnswers(crn)

      assertThat(returnValue).hasSize(1)
      assertThat(returnValue).containsKey(AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES)
    }

    @Test
    fun `Should return empty List if no Complexity Answers match`() {
      val crn = "123"
      val assessmentId = "1234"
      val complexityAnswers =
        listOf(
          Question(
            "15.3",
            setOf(Answer("Yes"))
          )
        )

      every { assessmentApiClient.getLatestAssessmentId(crn) } returns assessmentId
      every {
        assessmentApiClient.getAssessmentAnswers(
          assessmentId
        )
      } returns complexityAnswers
      val returnValue = assessmentService.getAssessmentComplexityAnswers(crn)

      assertThat(returnValue).isEmpty()
    }

    @Test
    fun `Should return Complexity any Answers Match`() {
      val crn = "123"
      val assessmentId = "1234"
      val complexityAnswers =
        listOf(
          Question(
            "13.3 - F",
            setOf(Answer("No"), Answer("No"), Answer("Yes"))
          )
        )

      every { assessmentApiClient.getLatestAssessmentId(crn) } returns assessmentId
      every {
        assessmentApiClient.getAssessmentAnswers(
          assessmentId
        )
      } returns complexityAnswers
      val returnValue = assessmentService.getAssessmentComplexityAnswers(crn)

      assertThat(returnValue).hasSize(1)
      assertThat(returnValue).containsKey(AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES)
    }

    @Test
    fun `Should return multiple Complexity Answers`() {
      val crn = "123"
      val assessmentId = "1234"
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

      every { assessmentApiClient.getLatestAssessmentId(crn) } returns assessmentId
      every {
        assessmentApiClient.getAssessmentAnswers(
          assessmentId
        )
      } returns complexityAnswers
      val returnValue = assessmentService.getAssessmentComplexityAnswers(crn)

      assertThat(returnValue).hasSize(2)
      assertThat(returnValue).containsKey(AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES)
      assertThat(returnValue).containsKey(AssessmentComplexityFactor.TEMPER_CONTROL)
    }

    @Test
    fun `Should return empty List if no Complexity Answers present`() {
      val crn = "123"
      val assessmentId = "1234"
      val complexityAnswers =
        listOf<Question>()

      every { assessmentApiClient.getLatestAssessmentId(crn) } returns assessmentId
      every {
        assessmentApiClient.getAssessmentAnswers(
          assessmentId
        )
      } returns complexityAnswers
      val returnValue = assessmentService.getAssessmentComplexityAnswers(crn)

      assertThat(returnValue).isEmpty()
    }

    @Test
    fun `Should throw if no Latest Assessment`() {
      val crn = "123"

      every { assessmentApiClient.getLatestAssessmentId(crn) } returns null

      assertThrows<EntityNotFoundException> {
        assessmentService.getAssessmentComplexityAnswers(crn)
      }
    }
  }

  @Nested
  @DisplayName("Get Needs Tests")
  inner class GetNeedsTests {

    @Test
    fun `Should throw if no Latest Assessment`() {
      val crn = "123"

      every { assessmentApiClient.getLatestAssessmentId(crn) } returns null

      assertThrows<EntityNotFoundException> {
        assessmentService.getAssessmentNeeds(crn)
      }
    }

    @Test
    fun `Should return empty Map if no Needs`() {
      val crn = "123"
      val assessmentId = "1234"
      val needs = listOf<AssessmentNeed>()

      every { assessmentApiClient.getLatestAssessmentId(crn) } returns assessmentId
      every { assessmentApiClient.getAssessmentNeeds(assessmentId) } returns needs
      val returnValue = assessmentService.getAssessmentNeeds(crn)

      assertThat(returnValue).isEmpty()
    }

    @Test
    fun `Should return Needs`() {
      val crn = "123"
      val assessmentId = "1234"
      val needs = listOf(
        AssessmentNeed(
          Need.ACCOMMODATION,
          NeedSeverity.NO_NEED
        )
      )

      every { assessmentApiClient.getLatestAssessmentId(crn) } returns assessmentId
      every { assessmentApiClient.getAssessmentNeeds(assessmentId) } returns needs
      val returnValue = assessmentService.getAssessmentNeeds(crn)

      assertThat(returnValue).hasSize(1)
      assertThat(returnValue).containsEntry(Need.ACCOMMODATION, NeedSeverity.NO_NEED)
    }
  }
}
