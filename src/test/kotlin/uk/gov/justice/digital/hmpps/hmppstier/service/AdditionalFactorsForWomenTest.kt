package uk.gov.justice.digital.hmpps.hmppstier.service

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
class AdditionalFactorsForWomenTest {
    private val assessmentApiService: AssessmentApiService = mockk(relaxUnitFun = true)
    private val additionalFactorsForWomen: AdditionalFactorsForWomen = AdditionalFactorsForWomen(
        assessmentApiService,
    )

    @BeforeEach
    fun resetAllMocks() {
        clearMocks(assessmentApiService)
    }

    @AfterEach
    fun confirmVerified() {
        // Check we don't add any more calls without updating the tests
        confirmVerified(assessmentApiService)
    }

    @Test
    fun `should not count assessment additional factors duplicates`() = runBlocking {
        val assessment = OffenderAssessment("12345", LocalDateTime.now(), null, "AnyStatus")

        coEvery { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
            mapOf(
                AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
                AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
                AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
                AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
                AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
                AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
            )
        val result = additionalFactorsForWomen.calculate(
            offenderAssessment = assessment,
            offenderIsFemale = true,
            previousEnforcementActivity = false,
        )
        assertThat(result).isEqualTo(2)

        coVerify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should not count assessment additional factors duplicates mixed answers`() = runBlocking {
        val assessment = OffenderAssessment("12345", LocalDateTime.now(), null, "AnyStatus")

        coEvery { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
            mapOf(
                AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "YES",
                AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
            )
        val result = additionalFactorsForWomen.calculate(
            offenderAssessment = assessment,
            offenderIsFemale = true,
            previousEnforcementActivity = false,
        )
        assertThat(result).isEqualTo(2)

        coVerify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should add multiple additional factors`() = runBlocking {
        val assessment = OffenderAssessment("12345", LocalDateTime.now(), null, "AnyStatus")

        coEvery { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
            mapOf(
                AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
                AdditionalFactorForWomen.TEMPER_CONTROL to "1",
            )
        val result = additionalFactorsForWomen.calculate(
            offenderAssessment = assessment,
            offenderIsFemale = true,
            previousEnforcementActivity = false,
        )
        assertThat(result).isEqualTo(4)

        coVerify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should not include additional factors if no valid assessment`(): Unit = runBlocking {
        val assessment = null

        val result = additionalFactorsForWomen.calculate(
            offenderAssessment = assessment,
            offenderIsFemale = true,
            previousEnforcementActivity = false,
        )
        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `should count both Temper and Impulsivity as max '1'`() = runBlocking {
        val assessment = OffenderAssessment("12345", LocalDateTime.now(), null, "AnyStatus")

        coEvery { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
            mapOf(
                AdditionalFactorForWomen.IMPULSIVITY to "2",
                AdditionalFactorForWomen.TEMPER_CONTROL to "1",
            )
        val result = additionalFactorsForWomen.calculate(
            offenderAssessment = assessment,
            offenderIsFemale = true,
            previousEnforcementActivity = false,
        )
        assertThat(result).isEqualTo(2) // 1 * 2 weighting for all additional factors

        coVerify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should count Temper without Impulsivity as max '2'`() = runBlocking {
        val assessment = OffenderAssessment("12345", LocalDateTime.now(), null, "AnyStatus")

        coEvery { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
            mapOf(
                AdditionalFactorForWomen.TEMPER_CONTROL to "1",
            )
        val result = additionalFactorsForWomen.calculate(
            offenderAssessment = assessment,
            offenderIsFemale = true,
            previousEnforcementActivity = false,
        )
        assertThat(result).isEqualTo(2) // 1 * 2 weighting for all additional factors

        coVerify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should count Impulsivity without Temper as max '1'`() = runBlocking {
        val assessment = OffenderAssessment("12345", LocalDateTime.now(), null, "AnyStatus")

        coEvery { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
            mapOf(
                AdditionalFactorForWomen.IMPULSIVITY to "2",
            )
        val result = additionalFactorsForWomen.calculate(
            offenderAssessment = assessment,
            offenderIsFemale = true,
            previousEnforcementActivity = false,
        )
        assertThat(result).isEqualTo(2) // 1 * 2 weighting for all additional factors

        coVerify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should ignore negative Parenting`() = runBlocking {
        val assessment = OffenderAssessment("12345", LocalDateTime.now(), null, "AnyStatus")

        coEvery { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
            mapOf(
                AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "N",
            )
        val result = additionalFactorsForWomen.calculate(
            offenderAssessment = assessment,
            offenderIsFemale = true,
            previousEnforcementActivity = false,
        )
        assertThat(result).isEqualTo(0)

        coVerify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should ignore negative Impulsivity`() = runBlocking {
        val assessment = OffenderAssessment("12345", LocalDateTime.now(), null, "AnyStatus")

        coEvery { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
            mapOf(
                AdditionalFactorForWomen.IMPULSIVITY to "0",
            )
        val result = additionalFactorsForWomen.calculate(
            offenderAssessment = assessment,
            offenderIsFemale = true,
            previousEnforcementActivity = false,
        )
        assertThat(result).isEqualTo(0)

        coVerify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should ignore negative Temper`() = runBlocking {
        val assessment = OffenderAssessment("12345", LocalDateTime.now(), null, "AnyStatus")

        coEvery { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
            mapOf(
                AdditionalFactorForWomen.TEMPER_CONTROL to "0",
            )
        val result = additionalFactorsForWomen.calculate(
            offenderAssessment = assessment,
            offenderIsFemale = true,
            previousEnforcementActivity = false,
        )
        assertThat(result).isEqualTo(0)

        coVerify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `Should return Breach points if previousEnforcementActivity is true`(): Unit = runBlocking {
        val result = additionalFactorsForWomen.calculate(
            offenderAssessment = null,
            offenderIsFemale = true,
            previousEnforcementActivity = true,
        )
        assertThat(result).isEqualTo(2)
    }

    @Test
    fun `Should return no Breach points if previousEnforcementActivity is false`(): Unit = runBlocking {
        val result = additionalFactorsForWomen.calculate(
            offenderAssessment = null,
            offenderIsFemale = true,
            previousEnforcementActivity = false,
        )
        assertThat(result).isEqualTo(0)
    }
}
