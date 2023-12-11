package uk.gov.justice.digital.hmpps.hmppstier.service

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
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
    private val clock =
        Clock.fixed(LocalDate.of(2021, 1, 20).atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())
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
        fun `Should return Answer if present and positive`() = runBlocking {
            val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null, "COMPLETE")
            val answers =
                listOf(
                    Question(
                        "6.9",
                        setOf(Answer("Yes")),
                    ),
                )

            coEvery {
                assessmentApiClient.getAssessmentAnswers(
                    assessment.assessmentId,
                )
            } returns answers
            val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

            assertThat(returnValue).hasSize(1)
            assertThat(returnValue).containsKey(AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES)

            coVerify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
        }

        @Test
        fun `Should return Answer even if present and negative`() = runBlocking {
            val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null, "COMPLETE")
            val answers =
                listOf(
                    Question(
                        "6.9",
                        setOf(Answer("No")),
                    ),
                )

            coEvery {
                assessmentApiClient.getAssessmentAnswers(
                    assessment.assessmentId,
                )
            } returns answers
            val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

            assertThat(returnValue).hasSize(1)
            assertThat(returnValue).containsKey(AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES)

            coVerify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
        }

        @Test
        fun `Should match Answer Case Insensitive Question`() = runBlocking {
            val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null, "COMPLETE")
            val answers =
                listOf(
                    Question(
                        "6.9",
                        setOf(Answer("Yes")),
                    ),
                )

            coEvery {
                assessmentApiClient.getAssessmentAnswers(
                    assessment.assessmentId,
                )
            } returns answers
            val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

            assertThat(returnValue).hasSize(1)
            assertThat(returnValue).containsKey(AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES)

            coVerify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
        }

        @Test
        fun `Should match Answer Case Insensitive Answer`() = runBlocking {
            val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null, "COMPLETE")
            val answers =
                listOf(
                    Question(
                        "6.9",
                        setOf(Answer("YeS")),
                    ),
                )

            coEvery {
                assessmentApiClient.getAssessmentAnswers(
                    assessment.assessmentId,
                )
            } returns answers
            val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

            assertThat(returnValue).hasSize(1)
            assertThat(returnValue).containsKey(AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES)

            coVerify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
        }

        @Test
        fun `Should return empty List if no Answers match`() = runBlocking {
            val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null, "COMPLETE")
            val answers =
                listOf(
                    Question(
                        "15.3",
                        setOf(Answer("Yes")),
                    ),
                )

            coEvery {
                assessmentApiClient.getAssessmentAnswers(
                    assessment.assessmentId,
                )
            } returns answers
            val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

            assertThat(returnValue).isEmpty()
            coVerify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
        }

        @Test
        fun `Should return any Answers Match`() = runBlocking {
            val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null, "COMPLETE")
            val answers =
                listOf(
                    Question(
                        "6.9",
                        setOf(Answer("No"), Answer("No"), Answer("Yes")),
                    ),
                )

            coEvery { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) } returns answers
            val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

            assertThat(returnValue).hasSize(1)
            assertThat(returnValue).containsKey(AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES)

            coVerify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
        }

        @Test
        fun `Should return multiple Answers`() = runBlocking {
            val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null, "COMPLETE")
            val answers =
                listOf(
                    Question(
                        "6.9",
                        setOf(Answer("No"), Answer("No"), Answer("Yes")),
                    ),
                    Question(
                        "11.4",
                        setOf(Answer("Yes")),
                    ),
                )

            coEvery {
                assessmentApiClient.getAssessmentAnswers(
                    assessment.assessmentId,
                )
            } returns answers
            val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

            assertThat(returnValue).hasSize(2)
            assertThat(returnValue).containsKey(AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES)
            assertThat(returnValue).containsKey(AdditionalFactorForWomen.TEMPER_CONTROL)

            coVerify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
        }

        @Test
        fun `Should return empty List if no Complexity Answers present`() = runBlocking {
            val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null, "COMPLETE")
            val answers =
                listOf<Question>()

            coEvery {
                assessmentApiClient.getAssessmentAnswers(
                    assessment.assessmentId,
                )
            } returns answers
            val returnValue = assessmentService.getAssessmentAnswers(assessment.assessmentId)

            assertThat(returnValue).isEmpty()

            coVerify { assessmentApiClient.getAssessmentAnswers(assessment.assessmentId) }
        }
    }

    @Nested
    @DisplayName("Get Needs Tests")
    inner class GetNeedsTests {

        @Test
        fun `Should return empty Map if no Needs`() = runBlocking {
            val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null, "COMPLETE")
            val needs = listOf<AssessmentNeed>()

            coEvery { assessmentApiClient.getAssessmentNeeds(assessment.assessmentId) } returns needs
            val returnValue = assessmentService.getAssessmentNeeds(assessment)

            assertThat(returnValue).isEmpty()

            coVerify { assessmentApiClient.getAssessmentNeeds(assessment.assessmentId) }
        }

        @Test
        fun `Should return Needs`() = runBlocking {
            val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null, "COMPLETE")
            val needs = listOf(
                AssessmentNeed(
                    Need.ACCOMMODATION,
                    NeedSeverity.NO_NEED,
                ),
            )

            coEvery { assessmentApiClient.getAssessmentNeeds(assessment.assessmentId) } returns needs
            val returnValue = assessmentService.getAssessmentNeeds(assessment)

            assertThat(returnValue).hasSize(1)
            assertThat(returnValue).containsEntry(Need.ACCOMMODATION, NeedSeverity.NO_NEED)

            coVerify { assessmentApiClient.getAssessmentNeeds(assessment.assessmentId) }
        }

        @Test
        fun `Should return Multiple Needs`() = runBlocking {
            val assessment = OffenderAssessment("1234", LocalDateTime.now(clock), null, "COMPLETE")
            val needs = listOf(
                AssessmentNeed(
                    Need.ACCOMMODATION,
                    NeedSeverity.NO_NEED,
                ),
                AssessmentNeed(
                    Need.ALCOHOL_MISUSE,
                    NeedSeverity.SEVERE,
                ),
            )

            coEvery { assessmentApiClient.getAssessmentNeeds(assessment.assessmentId) } returns needs
            val returnValue = assessmentService.getAssessmentNeeds(assessment)

            assertThat(returnValue).hasSize(2)
            assertThat(returnValue).containsEntry(Need.ACCOMMODATION, NeedSeverity.NO_NEED)
            assertThat(returnValue).containsEntry(Need.ALCOHOL_MISUSE, NeedSeverity.SEVERE)

            coVerify { assessmentApiClient.getAssessmentNeeds(assessment.assessmentId) }
        }
    }

    @Nested
    @DisplayName("Get recent Assessment Tests")
    inner class GetRecentAssessmentTests {

        @Test
        fun `Should return if inside Threshold`() = runBlocking {
            val crn = "123"
            val assessment = OffenderAssessment("1234", LocalDateTime.now(clock).minusWeeks(55), null, "COMPLETE")

            coEvery { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment)
            val returnValue = assessmentService.getRecentAssessment(crn)

            assertThat(returnValue).isNotNull

            coVerify { assessmentApiClient.getAssessmentSummaries(crn) }
        }

        @Test
        fun `Should return none if outside Threshold`() = runBlocking {
            val crn = "123"
            val assessment =
                OffenderAssessment("1234", LocalDateTime.now(clock).minusWeeks(55).minusDays(1), null, "COMPLETE")
            // more recent, but voided
            val voidedAssessment = OffenderAssessment(
                "1234",
                LocalDateTime.now(clock).minusWeeks(40),
                LocalDateTime.now(clock),
                "COMPLETE"
            )

            coEvery { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment, voidedAssessment)
            val returnValue = assessmentService.getRecentAssessment(crn)

            assertThat(returnValue).isNull()

            coVerify { assessmentApiClient.getAssessmentSummaries(crn) }
        }

        @Test
        fun `Should return none if voided`() = runBlocking {
            val crn = "123"
            val assessment = OffenderAssessment(
                "1234",
                LocalDateTime.now(clock).minusWeeks(55).minusDays(1),
                LocalDateTime.now(clock),
                "COMPLETE"
            )

            coEvery { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment)
            val returnValue = assessmentService.getRecentAssessment(crn)
            assertThat(returnValue).isNull()

            coVerify { assessmentApiClient.getAssessmentSummaries(crn) }
        }

        @Test
        fun `Should return none if not complete date`() = runBlocking {
            val crn = "123"
            val assessment = OffenderAssessment("1234", null, LocalDateTime.now(clock), "COMPLETE")

            coEvery { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment)
            val returnValue = assessmentService.getRecentAssessment(crn)
            assertThat(returnValue).isNull()

            coVerify { assessmentApiClient.getAssessmentSummaries(crn) }
        }

        @Test
        fun `Should return none if none valid`() = runBlocking {
            val crn = "123"

            coEvery { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf()
            val returnValue = assessmentService.getRecentAssessment(crn)
            assertThat(returnValue).isNull()

            coVerify { assessmentApiClient.getAssessmentSummaries(crn) }
        }

        @Test
        fun `Should return none if not complete status`() = runBlocking {
            val crn = "123"
            val assessment = OffenderAssessment("1234", null, LocalDateTime.now(clock), "OTHER_STATUS")

            coEvery { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment)
            val returnValue = assessmentService.getRecentAssessment(crn)
            assertThat(returnValue).isNull()

            coVerify { assessmentApiClient.getAssessmentSummaries(crn) }
        }

        @Test
        fun `Should return latest of two COMPLETED`() = runBlocking {
            val crn = "123"
            val assessment =
                OffenderAssessment("1234", LocalDateTime.now(clock).minusWeeks(55).minusDays(1), null, "COMPLETE")
            // more recent
            val assessmentNewer = OffenderAssessment("4321", LocalDateTime.now(clock).minusWeeks(40), null, "COMPLETE")

            coEvery { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment, assessmentNewer)
            val returnValue = assessmentService.getRecentAssessment(crn)

            assertThat(returnValue).isNotNull
            assertThat(returnValue!!.assessmentId).isEqualTo(assessmentNewer.assessmentId)

            coVerify { assessmentApiClient.getAssessmentSummaries(crn) }
        }

        @Test
        fun `Should return latest one LOCKED_INCOMPLETE`() = runBlocking {
            val crn = "123"
            val assessment =
                OffenderAssessment("1234", LocalDateTime.now(clock).minusWeeks(55).minusDays(1), null, "COMPLETE")
            // more recent
            val assessmentNewer =
                OffenderAssessment("4321", LocalDateTime.now(clock).minusWeeks(40), null, "LOCKED_INCOMPLETE")

            coEvery { assessmentApiClient.getAssessmentSummaries(crn) } returns listOf(assessment, assessmentNewer)
            val returnValue = assessmentService.getRecentAssessment(crn)

            assertThat(returnValue).isNotNull
            assertThat(returnValue!!.assessmentId).isEqualTo(assessmentNewer.assessmentId)

            coVerify { assessmentApiClient.getAssessmentSummaries(crn) }
        }
    }
}
