package uk.gov.justice.digital.hmpps.hmppstier.service

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.Offender
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class AdditionalFactorsForWomenTest {

  private val clock = Clock.fixed(LocalDateTime.of(2020, 1, 1, 0, 0).toInstant(ZoneOffset.UTC), ZoneId.systemDefault())
  private val communityApiClient: CommunityApiClient = mockk(relaxUnitFun = true)
  private val assessmentApiService: AssessmentApiService = mockk(relaxUnitFun = true)
  private val additionalFactorsForWomen: AdditionalFactorsForWomen = AdditionalFactorsForWomen(clock, communityApiClient, assessmentApiService)

  @BeforeEach
  fun resetAllMocks() {
    clearMocks(communityApiClient)
    clearMocks(assessmentApiService)
  }

  @AfterEach
  fun confirmVerified() {
    // Check we don't add any more calls without updating the tests
    io.mockk.confirmVerified(communityApiClient)
    io.mockk.confirmVerified(assessmentApiService)
  }
  @Nested
  @DisplayName("Additional Factors For Women tests")
  inner class AdditionalFactorsForWomenTests {
    private val crn = "Any Crn"

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

      val result = additionalFactorsForWomen.calculate(crn, listOf(), assessment,)
      Assertions.assertThat(result).isEqualTo(2)

      verify { communityApiClient.getOffender(crn) }
      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should not count assessment additional factors duplicates mixed answers`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { communityApiClient.getOffender(crn) } returns Offender("Female")
      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "YES",
          AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
        )

      val result = additionalFactorsForWomen.calculate(crn, listOf(), assessment,)
      Assertions.assertThat(result).isEqualTo(2)

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

      val result = additionalFactorsForWomen.calculate(crn, listOf(), assessment,)
      Assertions.assertThat(result).isEqualTo(4)

      verify { communityApiClient.getOffender(crn) }
      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should not include additional factors if no valid assessment`() {
      val assessment = null

      every { communityApiClient.getOffender(crn) } returns Offender("Female")

      val result = additionalFactorsForWomen.calculate(crn, listOf(), assessment,)
      Assertions.assertThat(result).isEqualTo(0)

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

      val result = additionalFactorsForWomen.calculate(crn, listOf(), assessment,)
      Assertions.assertThat(result).isEqualTo(2) // 1 * 2 weighting for all additional factors

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

      val result = additionalFactorsForWomen.calculate(crn, listOf(), assessment,)
      Assertions.assertThat(result).isEqualTo(2) // 1 * 2 weighting for all additional factors

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

      val result = additionalFactorsForWomen.calculate(crn, listOf(), assessment,)
      Assertions.assertThat(result).isEqualTo(2) // 1 * 2 weighting for all additional factors

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

      val result = additionalFactorsForWomen.calculate(crn, listOf(), assessment,)
      Assertions.assertThat(result).isEqualTo(0)

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

      val result = additionalFactorsForWomen.calculate(crn, listOf(), assessment,)
      Assertions.assertThat(result).isEqualTo(0)

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

      val result = additionalFactorsForWomen.calculate(crn, listOf(), assessment,)
      Assertions.assertThat(result).isEqualTo(0)

      verify { communityApiClient.getOffender(crn) }
      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }
  }
}
