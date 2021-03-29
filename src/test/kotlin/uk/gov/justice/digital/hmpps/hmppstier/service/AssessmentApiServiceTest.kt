package uk.gov.justice.digital.hmpps.hmppstier.service

import io.mockk.clearMocks
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
import uk.gov.justice.digital.hmpps.hmppstier.client.Answer
import uk.gov.justice.digital.hmpps.hmppstier.client.AssessmentApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.AssessmentNeed
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.client.Question
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
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

  @AfterEach
  fun confirmVerified() {
    // Check we don't add any more calls without updating the tests
    io.mockk.confirmVerified(assessmentApiClient)
  }

  @Nested
  @DisplayName("Get Additional Factors For Women Tests")
  inner class GetAdditionalFactorsForWomenTests {

    @Test
    fun `Should return Answer if present and positive`() {
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null, "COMPLETE")
      val answers =
        listOf(
          Question(
            "6.9",
            setOf(Answer("Yes"))
          )
        )

      every {
        assessmentApiClient.getAssessmentAnswers(
          assessment.assessmentId
        )
      } returns answers
      val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

      assertThat(returnValue).hasSize(1)
      assertThat(returnValue).containsKey(AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES)

      verify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `Should return Answer even if present and negative`() {
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null, "COMPLETE")
      val answers =
        listOf(
          Question(
            "6.9",
            setOf(Answer("No"))
          )
        )

      every {
        assessmentApiClient.getAssessmentAnswers(
          assessment.assessmentId
        )
      } returns answers
      val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

      assertThat(returnValue).hasSize(1)
      assertThat(returnValue).containsKey(AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES)

      verify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `Should match Answer Case Insensitive Question`() {
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null, "COMPLETE")
      val answers =
        listOf(
          Question(
            "6.9",
            setOf(Answer("Yes"))
          )
        )

      every {
        assessmentApiClient.getAssessmentAnswers(
          assessment.assessmentId
        )
      } returns answers
      val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

      assertThat(returnValue).hasSize(1)
      assertThat(returnValue).containsKey(AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES)

      verify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `Should match Answer Case Insensitive Answer`() {
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null, "COMPLETE")
      val answers =
        listOf(
          Question(
            "6.9",
            setOf(Answer("YeS"))
          )
        )

      every {
        assessmentApiClient.getAssessmentAnswers(
          assessment.assessmentId
        )
      } returns answers
      val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

      assertThat(returnValue).hasSize(1)
      assertThat(returnValue).containsKey(AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES)

      verify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `Should return empty List if no Answers match`() {
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null, "COMPLETE")
      val answers =
        listOf(
          Question(
            "15.3",
            setOf(Answer("Yes"))
          )
        )

      every {
        assessmentApiClient.getAssessmentAnswers(
          assessment.assessmentId
        )
      } returns answers
      val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

      assertThat(returnValue).isEmpty()
      verify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `Should return any Answers Match`() {
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null, "COMPLETE")
      val answers =
        listOf(
          Question(
            "6.9",
            setOf(Answer("No"), Answer("No"), Answer("Yes"))
          )
        )

      every { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) } returns answers
      val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

      assertThat(returnValue).hasSize(1)
      assertThat(returnValue).containsKey(AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES)

      verify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `Should return multiple Answers`() {
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null, "COMPLETE")
      val answers =
        listOf(
          Question(
            "6.9",
            setOf(Answer("No"), Answer("No"), Answer("Yes"))
          ),
          Question(
            "11.4",
            setOf(Answer("Yes"))
          )
        )

      every {
        assessmentApiClient.getAssessmentAnswers(
          assessment.assessmentId
        )
      } returns answers
      val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

      assertThat(returnValue).hasSize(2)
      assertThat(returnValue).containsKey(AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES)
      assertThat(returnValue).containsKey(AdditionalFactorForWomen.TEMPER_CONTROL)

      verify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `Should return empty List if no Complexity Answers present`() {
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null, "COMPLETE")
      val answers =
        listOf<Question>()

      every {
        assessmentApiClient.getAssessmentAnswers(
          assessment.assessmentId
        )
      } returns answers
      val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

      assertThat(returnValue).isEmpty()

      verify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
    }
  }

  @Nested
  @DisplayName("Get Needs Tests")
  inner class GetNeedsTests {

    @Test
    fun `Should return empty Map if no Needs`() {
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null, "COMPLETE")
      val needs = listOf<AssessmentNeed>()

      every { assessmentApiClient.getAssessmentNeeds(assessment.assessmentId) } returns needs
      val returnValue = assessmentService.getAssessmentNeeds(assessment.assessmentId)

      assertThat(returnValue).isEmpty()

      verify { assessmentApiClient.getAssessmentNeeds(assessment.assessmentId) }
    }

    @Test
    fun `Should return Needs`() {
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null, "COMPLETE")
      val needs = listOf(
        AssessmentNeed(
          Need.ACCOMMODATION,
          NeedSeverity.NO_NEED
        )
      )

      every { assessmentApiClient.getAssessmentNeeds(assessment.assessmentId) } returns needs
      val returnValue = assessmentService.getAssessmentNeeds(assessment.assessmentId)

      assertThat(returnValue).hasSize(1)
      assertThat(returnValue).containsEntry(Need.ACCOMMODATION, NeedSeverity.NO_NEED)

      verify { assessmentApiClient.getAssessmentNeeds(assessment.assessmentId) }
    }

    @Test
    fun `Should return Multiple Needs`() {
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null, "COMPLETE")
      val needs = listOf(
        AssessmentNeed(
          Need.ACCOMMODATION,
          NeedSeverity.NO_NEED
        ),
        AssessmentNeed(
          Need.ALCOHOL_MISUSE,
          NeedSeverity.SEVERE
        )
      )

      every { assessmentApiClient.getAssessmentNeeds(assessment.assessmentId) } returns needs
      val returnValue = assessmentService.getAssessmentNeeds(assessment.assessmentId)

      assertThat(returnValue).hasSize(2)
      assertThat(returnValue).containsEntry(Need.ACCOMMODATION, NeedSeverity.NO_NEED)
      assertThat(returnValue).containsEntry(Need.ALCOHOL_MISUSE, NeedSeverity.SEVERE)

      verify { assessmentApiClient.getAssessmentNeeds(assessment.assessmentId) }
    }
  }

  @Nested
  @DisplayName("Get recent Assessment Tests")
  inner class GetRecentAssessmentTests {

    @Test
    fun `Should return if inside Threshold`() {
      val crn = "123"
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock).minusWeeks(55), null, "COMPLETE")

      every { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment)
      val returnValue = assessmentService.getRecentAssessment(crn)

      assertThat(returnValue).isNotNull

      verify { assessmentApiClient.getAssessmentSummaries(crn) }
    }

    @Test
    fun `Should return none if outside Threshold`() {
      val crn = "123"
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock).minusWeeks(55).minusDays(1), null, "COMPLETE")
      // more recent, but voided
      val voidedAssessment = OffenderAssessment("1234", LocalDateTime.now(clock).minusWeeks(40), LocalDateTime.now(clock), "COMPLETE")

      every { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment, voidedAssessment)
      val returnValue = assessmentService.getRecentAssessment(crn)

      assertThat(returnValue).isNull()

      verify { assessmentApiClient.getAssessmentSummaries(crn) }
    }

    @Test
    fun `Should return none if voided`() {
      val crn = "123"
      val assessment = OffenderAssessment("1234", LocalDateTime.now(clock).minusWeeks(55).minusDays(1), LocalDateTime.now(clock), "COMPLETE")

      every { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment)
      val returnValue = assessmentService.getRecentAssessment(crn)
      assertThat(returnValue).isNull()

      verify { assessmentApiClient.getAssessmentSummaries(crn) }
    }

    @Test
    fun `Should return none if not complete date`() {
      val crn = "123"
      val assessment = OffenderAssessment("1234", null, LocalDateTime.now(clock), "COMPLETE")

      every { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment)
      val returnValue = assessmentService.getRecentAssessment(crn)
      assertThat(returnValue).isNull()

      verify { assessmentApiClient.getAssessmentSummaries(crn) }
    }

    @Test
    fun `Should return none if none valid`() {
      val crn = "123"

      every { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf()
      val returnValue = assessmentService.getRecentAssessment(crn)
      assertThat(returnValue).isNull()

      verify { assessmentApiClient.getAssessmentSummaries(crn) }
    }

    @Test
    fun `Should return none if not complete status`() {
      val crn = "123"
      val assessment = OffenderAssessment("1234", null, LocalDateTime.now(clock), "INCOMPLETE_LOCKED")

      every { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment)
      val returnValue = assessmentService.getRecentAssessment(crn)
      assertThat(returnValue).isNull()

      verify { assessmentApiClient.getAssessmentSummaries(crn) }
    }
  }
}
