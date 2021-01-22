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
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusAssessmentsDto
import uk.gov.justice.digital.hmpps.hmppstier.client.KeyValue
import uk.gov.justice.digital.hmpps.hmppstier.client.Registration
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import uk.gov.justice.digital.hmpps.hmppstier.client.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.client.Nsi
import uk.gov.justice.digital.hmpps.hmppstier.client.Sentence

@ExtendWith(MockKExtension::class)
@DisplayName("Community Api Service tests")
internal class CommunityApiDataServiceTest {
  private val communityApiClient: CommunityApiClient = mockk(relaxUnitFun = true)
  private val clock = Clock.fixed(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())
  private val deliusDataService = CommunityApiDataService(communityApiClient, clock)

  @BeforeEach
  fun resetAllMocks() {
    clearMocks(communityApiClient)
  }

  @Nested
  @DisplayName("Get RoSH Tests")
  inner class GetRoshTests {

    @Test
    fun `Should return RoSH level if present`() {
      val crn = "123"
      val registrations =
        listOf(
          Registration(
            KeyValue("RMRH", "Medium RoSH"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          )
        )

      every { communityApiClient.getRegistrations(crn) } returns registrations
      val returnValue = deliusDataService.getRosh(crn)

      assertThat(returnValue).isEqualTo(Rosh.MEDIUM)
    }

    @Test
    fun `Should return RoSH level Case Insensitive`() {
      val crn = "123"
      val registrations =
        listOf(
          Registration(
            KeyValue("rmrh", "Medium RoSH"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          )
        )

      every { communityApiClient.getRegistrations(crn) } returns registrations
      val returnValue = deliusDataService.getRosh(crn)

      assertThat(returnValue).isEqualTo(Rosh.MEDIUM)
    }

    @Test
    fun `Should return RoSH level if present as first value in list`() {
      val crn = "123"
      val registrations =
        listOf(
          Registration(
            KeyValue("RMRH", "Medium RoSH"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S", "Risk to Staff"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S", "Risk to Staff"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          ),
        )

      every { communityApiClient.getRegistrations(crn) } returns registrations
      val returnValue = deliusDataService.getRosh(crn)

      assertThat(returnValue).isEqualTo(Rosh.MEDIUM)
    }

    @Test
    fun `Should return RoSH level if present as middle value in list`() {
      val crn = "123"
      val registrations =
        listOf(
          Registration(
            KeyValue("AV2S", "Risk to Staff"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          ),
          Registration(
            KeyValue("RMRH", "Medium RoSH"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S", "Risk to Staff"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          ),

        )

      every { communityApiClient.getRegistrations(crn) } returns registrations
      val returnValue = deliusDataService.getRosh(crn)

      assertThat(returnValue).isEqualTo(Rosh.MEDIUM)
    }

    @Test
    fun `Should return latest RoSH`() {
      val crn = "123"

      val registrations =
        listOf(
          Registration(
            KeyValue("RMRH", "Medium RoSH"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          ),
          Registration(
            KeyValue("RHRH", "High RoSH"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now().plusDays(1)
          ),
        )

      every { communityApiClient.getRegistrations(crn) } returns registrations
      val returnValue = deliusDataService.getRosh(crn)

      assertThat(returnValue).isEqualTo(Rosh.HIGH)
    }

    @Test
    fun `Should ignore inactive RoSH`() {
      val crn = "123"

      val registrations =
        listOf(
          Registration(
            KeyValue("RLRH", "Low RoSH"),
            KeyValue("Not", "Used"),
            false,
            LocalDate.now().plusDays(1)
          ),
          Registration(
            KeyValue("RMRH", "Medium RoSH"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          ),
        )

      every { communityApiClient.getRegistrations(crn) } returns registrations
      val returnValue = deliusDataService.getRosh(crn)

      assertThat(returnValue).isEqualTo(Rosh.MEDIUM)
    }

    @Test
    fun `Should return null if no RoSH if present`() {
      val crn = "123"
      val registrations =
        listOf(
          Registration(
            KeyValue("AV2S", "Risk to Staff"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          )
        )

      every { communityApiClient.getRegistrations(crn) } returns registrations
      val returnValue = deliusDataService.getRosh(crn)

      assertThat(returnValue).isNull()
    }
  }

  @Nested
  @DisplayName("Get Mappa Tests")
  inner class GetMappaTests {

    @Test
    fun `Should return Mappa level if present`() {
      val crn = "123"
      val registrations =
        listOf(
          Registration(
            KeyValue("Not", "Used"),
            KeyValue("M3", "One"),
            true,
            LocalDate.now()
          )
        )

      every { communityApiClient.getRegistrations(crn) } returns registrations
      val returnValue = deliusDataService.getMappa(crn)

      assertThat(returnValue).isEqualTo(Mappa.M3)
    }

    @Test
    fun `Should return Mappa level Case Insensitive`() {
      val crn = "123"
      val registrations =
        listOf(
          Registration(
            KeyValue("Not", "Used"),
            KeyValue("m3", "One"),
            true,
            LocalDate.now()
          )
        )

      every { communityApiClient.getRegistrations(crn) } returns registrations
      val returnValue = deliusDataService.getMappa(crn)

      assertThat(returnValue).isEqualTo(Mappa.M3)
    }

    @Test
    fun `Should return Mappa level if present as first value in list`() {
      val crn = "123"
      val registrations =
        listOf(
          Registration(
            KeyValue("Not", "Used"),
            KeyValue("M3", "One"),
            true,
            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S", "Risk to Staff"),
            KeyValue("BD", "OTHER"),
            true,
            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S", "Risk to Staff"),
            KeyValue("12", "ANOTHER"),
            true,
            LocalDate.now()
          ),

        )

      every { communityApiClient.getRegistrations(crn) } returns registrations
      val returnValue = deliusDataService.getMappa(crn)

      assertThat(returnValue).isEqualTo(Mappa.M3)
    }

    @Test
    fun `Should return Mappa level if present as middle value in list`() {
      val crn = "123"
      val registrations =
        listOf(
          Registration(
            KeyValue("AV2S", "Risk to Staff"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          ),
          Registration(
            KeyValue("Not", "Used"),
            KeyValue("M3", "One"),
            true,
            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S", "Risk to Staff"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          ),

        )

      every { communityApiClient.getRegistrations(crn) } returns registrations
      val returnValue = deliusDataService.getMappa(crn)

      assertThat(returnValue).isEqualTo(Mappa.M3)
    }

    @Test
    fun `Should return latest Mappa`() {
      val crn = "123"

      val registrations =
        listOf(
          Registration(
            KeyValue("Not", "Used"),
            KeyValue("M3", "One"),
            true,
            LocalDate.now()
          ),
          Registration(
            KeyValue("Not", "Used"),
            KeyValue("M2", "One"),
            true,
            LocalDate.now().plusDays(1)
          ),
        )

      every { communityApiClient.getRegistrations(crn) } returns registrations
      val returnValue = deliusDataService.getMappa(crn)

      assertThat(returnValue).isEqualTo(Mappa.M2)
    }

    @Test
    fun `Should ignore inactive Mappa`() {
      val crn = "123"

      val registrations =
        listOf(
          Registration(
            KeyValue("Not", "Used"),
            KeyValue("M2", "One"),
            false,
            LocalDate.now().plusDays(1)
          ),
          Registration(
            KeyValue("Not", "Used"),
            KeyValue("M3", "One"),
            true,
            LocalDate.now()
          ),
        )

      every { communityApiClient.getRegistrations(crn) } returns registrations
      val returnValue = deliusDataService.getMappa(crn)

      assertThat(returnValue).isEqualTo(Mappa.M3)
    }

    @Test
    fun `Should return null for invalid Mappa code`() {
      val crn = "123"
      val registrations =
        listOf(
          Registration(
            KeyValue("AV2S", "Risk to Staff"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S", "Risk to Staff"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          ),
          Registration(
            KeyValue("Not", "Used"),
            KeyValue("INVALID", "INVALID Mappa"),
            true,
            LocalDate.now()
          ),
        )

      every { communityApiClient.getRegistrations(crn) } returns registrations

      val returnValue = deliusDataService.getMappa(crn)

      assertThat(returnValue).isNull()
    }

    @Test
    fun `Should return null if no Mappa if present`() {
      val crn = "123"
      val registrations =
        listOf(
          Registration(
            KeyValue("AV2S", "Risk to Staff"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          )
        )

      every { communityApiClient.getRegistrations(crn) } returns registrations
      val returnValue = deliusDataService.getMappa(crn)

      assertThat(returnValue).isNull()
    }
  }

  @Nested
  @DisplayName("Get Complexity Factor Tests")
  inner class GetComplexityFactorTests {

    @Test
    fun `Should return Complexity Factor if present`() {
      val crn = "123"
      val registrations =
        listOf(
          Registration(
            KeyValue("RMDO", "Mental Health"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          )
        )

      every { communityApiClient.getRegistrations(crn) } returns registrations
      val returnValue = deliusDataService.getComplexityFactors(crn)

      assertThat(returnValue).containsOnly(ComplexityFactor.MENTAL_HEALTH)
    }

    @Test
    fun `Should return Complexity Factor Case Insensitive`() {
      val crn = "123"
      val registrations =
        listOf(
          Registration(
            KeyValue("rmdo", "Mental Health"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          )
        )

      every { communityApiClient.getRegistrations(crn) } returns registrations
      val returnValue = deliusDataService.getComplexityFactors(crn)

      assertThat(returnValue).containsOnly(ComplexityFactor.MENTAL_HEALTH)
    }

    @Test
    fun `Should return Complexity Factor if present as first value in list`() {
      val crn = "123"
      val registrations =
        listOf(
          Registration(
            KeyValue("RMDO", "Mental Health"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S", "Risk to Staff"),
            KeyValue("BD", "OTHER"),
            true,
            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S", "Risk to Staff"),
            KeyValue("12", "ANOTHER"),
            true,
            LocalDate.now()
          ),

        )

      every { communityApiClient.getRegistrations(crn) } returns registrations
      val returnValue = deliusDataService.getComplexityFactors(crn)

      assertThat(returnValue).containsOnly(ComplexityFactor.MENTAL_HEALTH)
    }

    @Test
    fun `Should return Complexity Factor if present as middle value in list`() {
      val crn = "123"
      val registrations =
        listOf(
          Registration(
            KeyValue("AV2S", "Risk to Staff"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          ),
          Registration(
            KeyValue("RMDO", "Mental Health"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S", "Risk to Staff"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          ),

        )

      every { communityApiClient.getRegistrations(crn) } returns registrations
      val returnValue = deliusDataService.getComplexityFactors(crn)

      assertThat(returnValue).containsOnly(ComplexityFactor.MENTAL_HEALTH)
    }

    @Test
    fun `Should ignore inactive Complexity Factor`() {
      val crn = "123"

      val registrations =
        listOf(
          Registration(
            KeyValue("RCHD", "RISK TO CHILDREN"),
            KeyValue("Not", "Used"),
            false,
            LocalDate.now().plusDays(1)
          ),
          Registration(
            KeyValue("RMDO", "Mental Health"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          ),
        )

      every { communityApiClient.getRegistrations(crn) } returns registrations
      val returnValue = deliusDataService.getComplexityFactors(crn)

      assertThat(returnValue).containsOnly(ComplexityFactor.MENTAL_HEALTH)
    }

    @Test
    fun `Should return empty List if no Complexity Factors present`() {
      val crn = "123"
      val registrations =
        listOf(
          Registration(
            KeyValue("AV2S", "Risk to Staff"),
            KeyValue("Not", "Used"),
            true,
            LocalDate.now()
          )
        )

      every { communityApiClient.getRegistrations(crn) } returns registrations
      val returnValue = deliusDataService.getComplexityFactors(crn)

      assertThat(returnValue).isEmpty()
    }
  }

  @Nested
  @DisplayName("Get RSR Tests")
  inner class GetRSRTests {

    @Test
    fun `Should return RSR`() {
      val crn = "123"
      val assessment = DeliusAssessmentsDto(
        rsr = BigDecimal(5),
        ogrs = null,
      )

      every { communityApiClient.getAssessments(crn) } returns assessment
      val returnValue = deliusDataService.getRSR(crn)

      assertThat(returnValue).isEqualTo(BigDecimal(5))
    }

    @Test
    fun `Should return RSR null`() {
      val crn = "123"
      val assessment = DeliusAssessmentsDto(
        rsr = null,
        ogrs = 10,
      )

      every { communityApiClient.getAssessments(crn) } returns assessment
      val returnValue = deliusDataService.getRSR(crn)

      assertThat(returnValue).isNull()
    }
  }

  @Nested
  @DisplayName("Get OGRS Tests")
  inner class GetOGRSTests {

    @Test
    fun `Should return OGRS`() {
      val crn = "123"
      val assessment = DeliusAssessmentsDto(
        rsr = null,
        ogrs = 50,
      )

      every { communityApiClient.getAssessments(crn) } returns assessment
      val returnValue = deliusDataService.getOGRS(crn)

      assertThat(returnValue).isEqualTo(50)
    }

    @Test
    fun `Should return OGRS null`() {
      val crn = "123"
      val assessment = DeliusAssessmentsDto(
        rsr = BigDecimal(10),
        ogrs = null,
      )

      every { communityApiClient.getAssessments(crn) } returns assessment
      val returnValue = deliusDataService.getOGRS(crn)

      assertThat(returnValue).isNull()
    }
  }

  @Nested
  @DisplayName("Get Breach Recall Tests")
  inner class GetBreachRecallTests {

    @Test
    fun `Should return Breach true if present and valid terminationDate`() {
      val crn = "123"
      val convictionId = 54321L
      val terminationDate = LocalDate.now(clock)
      val sentence = Sentence(terminationDate)
      val conviction = Conviction(convictionId, sentence)

      val breaches = listOf(Nsi(status = KeyValue("BRE08", "Unused")))

      every { communityApiClient.getConvictions(crn) } returns listOf(conviction)
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches

      val returnValue = deliusDataService.hasBreachedConvictions(crn)

      assertThat(returnValue).isTrue
    }

    @Test
    fun `Should return Breach true if present and valid terminationDate after cutoff`() {
      val crn = "123"
      val convictionId = 54321L
      val terminationDate = LocalDate.now(clock).minusYears(1)
      val sentence = Sentence(terminationDate)
      val conviction = Conviction(convictionId, sentence)

      val breaches = listOf(Nsi(status = KeyValue("BRE08", "Unused")))

      every { communityApiClient.getConvictions(crn) } returns listOf(conviction)
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches

      val returnValue = deliusDataService.hasBreachedConvictions(crn)

      assertThat(returnValue).isTrue
    }

    @Test
    fun `Should return Breach false if present and valid terminationDate on cutoff`() {
      val crn = "123"
      val convictionId = 54321L
      val terminationDate = LocalDate.now(clock).minusYears(12).minusDays(1)
      val sentence = Sentence(terminationDate)
      val conviction = Conviction(convictionId, sentence)

      val breaches = listOf(Nsi(status = KeyValue("BRE08", "Unused")))

      every { communityApiClient.getConvictions(crn) } returns listOf(conviction)
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches

      val returnValue = deliusDataService.hasBreachedConvictions(crn)

      assertThat(returnValue).isFalse
    }

    @Test
    fun `Should return Breach true if present and valid not terminated`() {
      val crn = "123"
      val convictionId = 54321L
      val sentence = Sentence(null)
      val conviction = Conviction(convictionId, sentence)

      val breaches = listOf(Nsi(status = KeyValue("BRE08", "Unused")))

      every { communityApiClient.getConvictions(crn) } returns listOf(conviction)
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches

      val returnValue = deliusDataService.hasBreachedConvictions(crn)

      assertThat(returnValue).isTrue
    }

    @Test
    fun `Should return Breach true if multiple convictions, one valid`() {
      val crn = "123"
      val convictionId = 54321L
      val sentence = Sentence(null)
      val conviction = Conviction(convictionId, sentence)

      val unrelatedConviction = Conviction(convictionId.plus(1), sentence)
      val unrelatedBreaches = listOf(Nsi(status = KeyValue("BRE99", "Unused")))

      val breaches = listOf(Nsi(status = KeyValue("BRE08", "Unused")))

      every { communityApiClient.getConvictions(crn) } returns listOf(unrelatedConviction,conviction)
      every { communityApiClient.getBreachRecallNsis(crn, convictionId.plus(1)) } returns unrelatedBreaches
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches

      val returnValue = deliusDataService.hasBreachedConvictions(crn)

      assertThat(returnValue).isTrue
    }

    @Test
    fun `Should return Breach false if no conviction`() {
      val crn = "123"

      every { communityApiClient.getConvictions(crn) } returns listOf()

      val returnValue = deliusDataService.hasBreachedConvictions(crn)

      assertThat(returnValue).isFalse
    }

    @Test
    fun `Should return Breach true if one conviction, multiple breaches, one valid`() {
      val crn = "123"
      val convictionId = 54321L
      val sentence = Sentence(null)
      val conviction = Conviction(convictionId, sentence)

      val breaches = listOf(
        Nsi(status = KeyValue("BRE54", "Unused")),
        Nsi(status = KeyValue("BRE08", "Unused"))
      )

      every { communityApiClient.getConvictions(crn) } returns listOf(conviction)
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches

      val returnValue = deliusDataService.hasBreachedConvictions(crn)

      assertThat(returnValue).isTrue
    }

    @Test
    fun `Should return Breach true if one conviction, multiple breaches, one valid case insensitive`() {
      val crn = "123"
      val convictionId = 54321L
      val sentence = Sentence(null)
      val conviction = Conviction(convictionId, sentence)

      val breaches = listOf(
        Nsi(status = KeyValue("BRE54", "Unused")),
        Nsi(status = KeyValue("bre08", "Unused"))
      )

      every { communityApiClient.getConvictions(crn) } returns listOf(conviction)
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches

      val returnValue = deliusDataService.hasBreachedConvictions(crn)

      assertThat(returnValue).isTrue
    }

    @Test
    fun `Should return Breach true if one conviction, multiple breaches, multiple valid`() {
      val crn = "123"
      val convictionId = 54321L
      val sentence = Sentence(null)
      val conviction = Conviction(convictionId, sentence)

      val breaches = listOf(
        Nsi(status = KeyValue("BRE09", "Unused")),
        Nsi(status = KeyValue("BRE08", "Unused"))
      )

      every { communityApiClient.getConvictions(crn) } returns listOf(conviction)
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches

      val returnValue = deliusDataService.hasBreachedConvictions(crn)

      assertThat(returnValue).isTrue
    }

    @Test
    fun `Should return Breach false if one conviction, multiple breaches, none valid`() {
      val crn = "123"
      val convictionId = 54321L
      val sentence = Sentence(null)
      val conviction = Conviction(convictionId, sentence)

      val breaches = listOf(
        Nsi(status = KeyValue("BRE99", "Unused")),
        Nsi(status = KeyValue("BRE99", "Unused"))
      )

      every { communityApiClient.getConvictions(crn) } returns listOf(conviction)
      every { communityApiClient.getBreachRecallNsis(crn, convictionId) } returns breaches

      val returnValue = deliusDataService.hasBreachedConvictions(crn)

      assertThat(returnValue).isFalse
    }

  }
}
