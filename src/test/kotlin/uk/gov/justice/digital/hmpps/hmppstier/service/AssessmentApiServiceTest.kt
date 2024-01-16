package uk.gov.justice.digital.hmpps.hmppstier.service

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.hmppstier.client.*
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
    private val arnsApiClient: ArnsApiClient = mockk(relaxUnitFun = true)
    private val assessmentApiClient: AssessmentApiClient = mockk(relaxUnitFun = true)
    private val clock =
        Clock.fixed(LocalDate.of(2021, 1, 20).atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())
    private val assessmentService = AssessmentApiService(arnsApiClient, assessmentApiClient, clock)

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
        fun `Should return Answer even if present and negative`() {
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
        fun `Should match Answer Case Insensitive Question`() {
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
        fun `Should match Answer Case Insensitive Answer`() {
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
        fun `Should return empty List if no Answers match`() {
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
        fun `Should return any Answers Match`() {
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
        fun `Should return multiple Answers`() {
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
        fun `Should return empty List if no Complexity Answers present`() {
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
        fun `Should return empty Map if no Needs`() {
            val crn = "T123456"
            val assessedNeeds = AssessedNeeds()

            coEvery { arnsApiClient.getNeedsForCrn(crn) } returns assessedNeeds
            val returnValue = assessmentService.getAssessmentNeeds(crn)

            assertThat(returnValue).isEmpty()

            coVerify { arnsApiClient.getNeedsForCrn(crn) }
        }

        @Test
        fun `Should return Needs`() {
            val crn = "T123456"
            val needs = AssessedNeeds(
                identifiedNeeds = listOf(
                    AssessedNeed(
                        Need.ACCOMMODATION.toString(),
                        "Accommodation",
                        true,
                        false,
                        false,
                        false,
                        NeedSeverity.STANDARD,
                        true,
                        4
                    )
                ),
            )

            coEvery { arnsApiClient.getNeedsForCrn(crn) } returns needs
            val returnValue = assessmentService.getAssessmentNeeds(crn)

            assertThat(returnValue).hasSize(1)
            assertThat(returnValue).containsEntry(Need.ACCOMMODATION, NeedSeverity.STANDARD)
        }

        @Test
        fun `Should return Multiple Needs`() {
            val crn = "T123456"
            val needs = AssessedNeeds(
                identifiedNeeds = listOf(
                    AssessedNeed(
                        Need.ACCOMMODATION.toString(),
                        "Accommodation",
                        true,
                        false,
                        false,
                        true,
                        NeedSeverity.STANDARD,
                        true,
                        4
                    ),
                    AssessedNeed(
                        Need.ALCOHOL_MISUSE.toString(),
                        "Alcohol Misuse",
                        true,
                        true,
                        true,
                        true,
                        NeedSeverity.SEVERE,
                        true,
                        4
                    )
                ),
            )

            coEvery { arnsApiClient.getNeedsForCrn(crn) } returns needs
            val returnValue = assessmentService.getAssessmentNeeds(crn)

            assertThat(returnValue).hasSize(2)
            assertThat(returnValue).containsEntry(Need.ACCOMMODATION, NeedSeverity.STANDARD)
            assertThat(returnValue).containsEntry(Need.ALCOHOL_MISUSE, NeedSeverity.SEVERE)

            coVerify { arnsApiClient.getNeedsForCrn(crn) }
        }
    }

    @Nested
    @DisplayName("Get recent Assessment Tests")
    inner class GetRecentAssessmentTests {

        @Test
        fun `Should return if inside Threshold`() {
            val crn = "T123456"
            val timeline = Timeline(
                listOf(
                    AssessmentSummary(1234, LocalDateTime.now(clock).minusWeeks(55), "LAYER3", "COMPLETE")
                )
            )

            coEvery { arnsApiClient.getTimeline(crn) } returns timeline
            val returnValue = assessmentService.getRecentAssessment(crn)

            assertThat(returnValue).isNotNull

            coVerify { arnsApiClient.getTimeline(crn) }
        }

        @Test
        fun `Should return none if outside Threshold`() {
            val crn = "T123456"
            val timeline = Timeline(
                listOf(
                    AssessmentSummary(1234, LocalDateTime.now(clock).minusWeeks(55).minusDays(2), "LAYER3", "COMPLETE")
                )
            )
            coEvery { arnsApiClient.getTimeline(crn) } returns timeline
            val returnValue = assessmentService.getRecentAssessment(crn)

            assertThat(returnValue).isNull()

            coVerify { arnsApiClient.getTimeline(crn) }
        }

        @Test
        fun `Should return none if not complete date`() {
            val crn = "T123456"
            val timeline = Timeline(
                listOf(
                    AssessmentSummary(1234, null, "LAYER3", "IN_PROGRESS")
                )
            )

            coEvery { arnsApiClient.getTimeline(crn) } returns timeline
            val returnValue = assessmentService.getRecentAssessment(crn)
            assertThat(returnValue).isNull()

            coVerify { arnsApiClient.getTimeline(crn) }
        }

        @Test
        fun `Should return latest of two COMPLETED`() {
            val crn = "T123456"
            val older = AssessmentSummary(1234, LocalDateTime.now(clock).minusWeeks(55), "LAYER3", "COMPLETE")
            val newer = AssessmentSummary(4321, LocalDateTime.now(clock).minusWeeks(40), "LAYER3", "COMPLETE")
            val timeline = Timeline(listOf(older, newer))

            coEvery { arnsApiClient.getTimeline(crn) } returns timeline
            val returnValue = assessmentService.getRecentAssessment(crn)

            assertThat(returnValue).isNotNull
            assertThat(returnValue!!.assessmentId).isEqualTo(newer.id.toString())

            coVerify { arnsApiClient.getTimeline(crn) }
        }

        @Test
        fun `Should return latest one LOCKED_INCOMPLETE`() {
            val crn = "T123456"
            val older = AssessmentSummary(1234, LocalDateTime.now(clock).minusWeeks(55), "LAYER3", "COMPLETE")
            val newer = AssessmentSummary(4321, LocalDateTime.now(clock).minusWeeks(40), "LAYER3", "LOCKED_INCOMPLETE")
            val timeline = Timeline(listOf(older, newer))

            coEvery { arnsApiClient.getTimeline(crn) } returns timeline
            val returnValue = assessmentService.getRecentAssessment(crn)

            assertThat(returnValue).isNotNull
            assertThat(returnValue!!.assessmentId).isEqualTo(newer.id.toString())

            coVerify { arnsApiClient.getTimeline(crn) }
        }
    }
}
