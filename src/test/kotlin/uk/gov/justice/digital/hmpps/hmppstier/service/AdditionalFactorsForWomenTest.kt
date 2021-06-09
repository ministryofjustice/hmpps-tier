package uk.gov.justice.digital.hmpps.hmppstier.service

import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.KeyValue
import uk.gov.justice.digital.hmpps.hmppstier.client.Nsi
import uk.gov.justice.digital.hmpps.hmppstier.client.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppstier.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.domain.Sentence
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class AdditionalFactorsForWomenTest {
  private val clock = Clock.fixed(LocalDateTime.of(2020, 1, 1, 0, 0).toInstant(ZoneOffset.UTC), ZoneId.systemDefault())
  private val communityApiClient: CommunityApiClient = mockk(relaxUnitFun = true)
  private val assessmentApiService: AssessmentApiService = mockk(relaxUnitFun = true)
  private val additionalFactorsForWomen: AdditionalFactorsForWomen = AdditionalFactorsForWomen(
    clock,
    assessmentApiService,
    CommunityApiService(communityApiClient)
  )

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
  @DisplayName("Additional Factors For Women tests")
  inner class AdditionalFactorsForWomenTests {
    private val crn = "Any Crn"

    @Test
    fun `should not count assessment additional factors duplicates`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
          AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
          AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
          AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
          AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
          AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
        )
      val result = additionalFactorsForWomen.calculate(
        crn,
        listOf(),
        assessment,
        true,
      )
      assertThat(result).isEqualTo(2)

      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should not count assessment additional factors duplicates mixed answers`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "YES",
          AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
        )
      val result = additionalFactorsForWomen.calculate(
        crn,
        listOf(),
        assessment,
        true
      )
      assertThat(result).isEqualTo(2)

      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should add multiple additional factors`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "Y",
          AdditionalFactorForWomen.TEMPER_CONTROL to "1",
        )
      val result = additionalFactorsForWomen.calculate(
        crn,
        listOf(),
        assessment,
        true,
      )
      assertThat(result).isEqualTo(4)

      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should not include additional factors if no valid assessment`() {
      val assessment = null

      val result = additionalFactorsForWomen.calculate(
        crn,
        listOf(),
        assessment,
        true,
      )
      assertThat(result).isEqualTo(0)
    }

    @Test
    fun `should count both Temper and Impulsivity as max '1'`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AdditionalFactorForWomen.IMPULSIVITY to "2",
          AdditionalFactorForWomen.TEMPER_CONTROL to "1",
        )
      val result = additionalFactorsForWomen.calculate(
        crn,
        listOf(),
        assessment,
        true,
      )
      assertThat(result).isEqualTo(2) // 1 * 2 weighting for all additional factors

      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should count Temper without Impulsivity as max '2'`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AdditionalFactorForWomen.TEMPER_CONTROL to "1",
        )
      val result = additionalFactorsForWomen.calculate(
        crn,
        listOf(),
        assessment,
        true,
      )
      assertThat(result).isEqualTo(2) // 1 * 2 weighting for all additional factors

      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should count Impulsivity without Temper as max '1'`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AdditionalFactorForWomen.IMPULSIVITY to "2",
        )
      val result = additionalFactorsForWomen.calculate(
        crn,
        listOf(),
        assessment,
        true,
      )
      assertThat(result).isEqualTo(2) // 1 * 2 weighting for all additional factors

      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should ignore negative Parenting`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES to "N",
        )
      val result = additionalFactorsForWomen.calculate(
        crn,
        listOf(),
        assessment,
        true,
      )
      assertThat(result).isEqualTo(0)

      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should ignore negative Impulsivity`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AdditionalFactorForWomen.IMPULSIVITY to "0",
        )
      val result = additionalFactorsForWomen.calculate(
        crn,
        listOf(),
        assessment,
        true,
      )
      assertThat(result).isEqualTo(0)

      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }

    @Test
    fun `should ignore negative Temper`() {
      val assessment = OffenderAssessment("12345", LocalDateTime.now(clock), null, "AnyStatus")

      every { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) } returns
        mapOf(
          AdditionalFactorForWomen.TEMPER_CONTROL to "0",
        )
      val result = additionalFactorsForWomen.calculate(
        crn,
        listOf(),
        assessment,
        true,
      )
      assertThat(result).isEqualTo(0)

      verify { assessmentApiService.getAssessmentAnswers(assessment.assessmentId) }
    }
  }

  @Nested
  @DisplayName("Get Breach Recall Tests")
  inner class GetBreachRecallTests {
    private val irrelevantSentenceType = "Irrelevant"

    @Test
    fun `Should return Breach true if present and valid terminationDate after cutoff`() {
      val crn = "123"
      val convictionId = 54321L
      val terminationDate = LocalDate.now(clock).minusYears(1)
      val sentence = Sentence(terminationDate, irrelevantSentenceType)
      val conviction = Conviction(convictionId, sentence, listOf())
      val breaches = listOf(Nsi(status = KeyValue("BRE08")))
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches
      val result = additionalFactorsForWomen.calculate(
        crn,
        listOf(conviction),
        null,
        true
      )
      assertThat(result).isEqualTo(2)
      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
    }

    @Test
    fun `Should return Breach false if present and valid terminationDate on cutoff`() {
      val crn = "123"
      val convictionId = 54321L
      val terminationDate = LocalDate.now(clock).minusYears(1).minusDays(1)
      val sentence = Sentence(terminationDate, irrelevantSentenceType)
      val conviction = Conviction(convictionId, sentence, listOf())

      val result = additionalFactorsForWomen.calculate(
        crn,
        listOf(conviction),
        null,
        true
      )
      assertThat(result).isEqualTo(0)
    }

    @Test
    fun `Should return Breach true if present and valid not terminated`() {
      val crn = "123"
      val convictionId = 54321L
      val sentence = Sentence(null, irrelevantSentenceType)
      val conviction = Conviction(convictionId, sentence, listOf())
      val breaches = listOf(Nsi(status = KeyValue("BRE08")))
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches

      val result = additionalFactorsForWomen.calculate(
        crn,
        listOf(conviction),
        null,
        true
      )
      assertThat(result).isEqualTo(2)
      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
    }

    @Test
    fun `Should return Breach true if multiple convictions, one valid`() {
      val crn = "123"
      val convictionId = 54321L
      val sentence = Sentence(null, irrelevantSentenceType)
      val conviction = Conviction(convictionId, sentence, listOf())
      val unrelatedConviction = Conviction(convictionId.plus(1), sentence, listOf())
      val unrelatedBreaches = listOf(Nsi(status = KeyValue("BRE99")))
      val breaches = listOf(Nsi(status = KeyValue("BRE08")))
      every { communityApiClient.getBreachRecallNsis(crn, convictionId.plus(1)) } returns unrelatedBreaches
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches

      val result = additionalFactorsForWomen.calculate(
        crn,
        listOf(conviction, unrelatedConviction),
        null,
        true
      )
      assertThat(result).isEqualTo(2)
      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
    }

    @Test
    fun `Should return Breach false if no conviction`() {
      val crn = "123"

      val result = additionalFactorsForWomen.calculate(
        crn,
        listOf(),
        null,
        true
      )
      assertThat(result).isEqualTo(0)
    }

    @Test
    fun `Should return Breach true if one conviction, multiple breaches, one valid`() {
      val crn = "123"
      val convictionId = 54321L
      val sentence = Sentence(null, irrelevantSentenceType)
      val conviction = Conviction(convictionId, sentence, listOf())
      val breaches = listOf(
        Nsi(status = KeyValue("BRE54")),
        Nsi(status = KeyValue("BRE08"))
      )
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches

      val result = additionalFactorsForWomen.calculate(
        crn,
        listOf(conviction),
        null,
        true
      )
      assertThat(result).isEqualTo(2)
      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
    }

    @Test
    fun `Should return Breach true if one conviction, multiple breaches, one valid case insensitive`() {
      val crn = "123"
      val convictionId = 54321L
      val sentence = Sentence(null, irrelevantSentenceType)
      val conviction = Conviction(convictionId, sentence, listOf())
      val breaches = listOf(
        Nsi(status = KeyValue("BRE54")),
        Nsi(status = KeyValue("bre08"))
      )
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches

      val result = additionalFactorsForWomen.calculate(
        crn,
        listOf(conviction),
        null,
        true
      )
      assertThat(result).isEqualTo(2)
      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
    }

    @Test
    fun `Should return Breach false if one conviction, multiple breaches, none valid`() {
      val crn = "123"
      val convictionId = 54321L
      val sentence = Sentence(null, irrelevantSentenceType)
      val conviction = Conviction(convictionId, sentence, listOf())
      val breaches = listOf(
        Nsi(status = KeyValue("BRE99")),
        Nsi(status = KeyValue("BRE99"))
      )
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches

      val result = additionalFactorsForWomen.calculate(
        crn,
        listOf(conviction),
        null,
        true
      )
      assertThat(result).isEqualTo(0)
      verify { communityApiClient.getBreachRecallNsis(crn, convictionId) }
    }
  }
}
