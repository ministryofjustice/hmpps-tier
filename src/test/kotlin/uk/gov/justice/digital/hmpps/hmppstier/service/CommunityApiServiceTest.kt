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
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.KeyValue
import uk.gov.justice.digital.hmpps.hmppstier.client.Registration
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
@DisplayName("Community Api Service tests")
internal class CommunityApiServiceTest {
  private val communityApiClient: CommunityApiClient = mockk(relaxUnitFun = true)
  private val communityApiService = CommunityApiService(communityApiClient)

  private val crn = "X123456"

  @BeforeEach
  fun resetAllMocks() {
    clearMocks(communityApiClient)
  }

  @AfterEach
  fun confirmVerified() {
    coVerify { communityApiClient.getRegistrations(crn) }
    // Check we don't add any more calls without updating the tests
    io.mockk.confirmVerified(communityApiClient)
  }

  @Nested
  @DisplayName("Simple ROSH tests")
  inner class SimpleRoshTests {

    @Test
    fun `should return null for No Rosh`() = runBlocking {
      coEvery { communityApiClient.getRegistrations(crn) } returns listOf()

      val result = communityApiService.getRegistrations(crn).rosh
      assertThat(result).isNull()
    }

    @Test
    fun `Should return RoSH level if present`() = runBlocking {
      val registrations =
        listOf(
          Registration(
            KeyValue("RMRH"),
            KeyValue("Not Used"),
            LocalDate.now()
          )
        )
      coEvery { communityApiClient.getRegistrations(crn) } returns registrations

      val result = communityApiService.getRegistrations(crn).rosh
      assertThat(result).isEqualTo(Rosh.MEDIUM)
    }

    @Test
    fun `Should return RoSH level Case Insensitive`() = runBlocking {
      val registrations =
        listOf(
          Registration(
            KeyValue("rmrh"),
            KeyValue("Not Used"),
            LocalDate.now()
          )
        )
      coEvery { communityApiClient.getRegistrations(crn) } returns registrations

      val result = communityApiService.getRegistrations(crn).rosh
      assertThat(result).isEqualTo(Rosh.MEDIUM)
    }

    @Test
    fun `Should return RoSH level if present as first value in list`() = runBlocking {
      val registrations =
        listOf(
          Registration(
            KeyValue("RMRH"),
            KeyValue("Not Used"),

            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S"),
            KeyValue("Not Used"),

            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S"),
            KeyValue("Not Used"),

            LocalDate.now()
          ),
        )
      coEvery { communityApiClient.getRegistrations(crn) } returns registrations

      val result = communityApiService.getRegistrations(crn).rosh
      assertThat(result).isEqualTo(Rosh.MEDIUM)
    }

    @Test
    fun `Should return RoSH level if present as middle value in list`() = runBlocking {
      val registrations =
        listOf(
          Registration(
            KeyValue("AV2S"),
            KeyValue("Not Used"),

            LocalDate.now()
          ),
          Registration(
            KeyValue("RMRH"),
            KeyValue("Not Used"),

            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S"),
            KeyValue("Not Used"),

            LocalDate.now()
          ),

        )
      coEvery { communityApiClient.getRegistrations(crn) } returns registrations

      val result = communityApiService.getRegistrations(crn).rosh
      assertThat(result).isEqualTo(Rosh.MEDIUM)
    }
  }

  @Nested
  @DisplayName("Simple Mappa tests")
  inner class SimpleMappaTests {

    @Test
    fun `should return null for No Mappa`() = runBlocking {
      coEvery { communityApiClient.getRegistrations(crn) } returns listOf()

      val result = communityApiService.getRegistrations(crn).mappa
      assertThat(result).isNull()
    }

    @Test
    fun `Should return Mappa level if present`() = runBlocking {
      val registrations =
        listOf(
          Registration(
            KeyValue("Not Used"),
            KeyValue("M3"),
            LocalDate.now()
          )
        )
      coEvery { communityApiClient.getRegistrations(crn) } returns registrations

      val result = communityApiService.getRegistrations(crn).mappa
      assertThat(result).isEqualTo(Mappa.M3)
    }

    @Test
    fun `Should return Mappa level Case Insensitive`() = runBlocking {
      val registrations =
        listOf(
          Registration(
            KeyValue("Not Used"),
            KeyValue("m3"),
            LocalDate.now()
          )
        )
      coEvery { communityApiClient.getRegistrations(crn) } returns registrations

      val result = communityApiService.getRegistrations(crn).mappa
      assertThat(result).isEqualTo(Mappa.M3)
    }

    @Test
    fun `Should return Mappa level if present as first value in list`() = runBlocking {
      val registrations =
        listOf(
          Registration(
            KeyValue("Not Used"),
            KeyValue("M3"),

            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S"),
            KeyValue("BD"),

            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S"),
            KeyValue("12"),

            LocalDate.now()
          ),

        )
      coEvery { communityApiClient.getRegistrations(crn) } returns registrations

      val result = communityApiService.getRegistrations(crn).mappa
      assertThat(result).isEqualTo(Mappa.M3)
    }

    @Test
    fun `Should return null for invalid Mappa code`() = runBlocking {
      val registrations =
        listOf(
          Registration(
            KeyValue("AV2S"),
            KeyValue("Not Used"),

            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S"),
            KeyValue("Not Used"),

            LocalDate.now()
          ),
          Registration(
            KeyValue("Not Used"),
            KeyValue("INVALID"),

            LocalDate.now()
          ),
        )
      coEvery { communityApiClient.getRegistrations(crn) } returns registrations

      val result = communityApiService.getRegistrations(crn).mappa
      assertThat(result).isNull()
    }
  }

  @Nested
  @DisplayName("Simple Complexity tests")
  inner class SimpleComplexityTests {

    @Test
    fun `should count complexity factors `() = runBlocking {
      coEvery { communityApiClient.getRegistrations(crn) } returns getValidRegistrations(
        listOf(
          ComplexityFactor.VULNERABILITY_ISSUE,
          ComplexityFactor.ADULT_AT_RISK,
        )
      )
      val result = communityApiService.getRegistrations(crn).complexityFactors
      assertThat(result.size).isEqualTo(2)
    }

    @Test
    fun `should not count complexity factors duplicates`() = runBlocking {
      coEvery { communityApiClient.getRegistrations(crn) } returns
        getValidRegistrations(
          listOf(
            ComplexityFactor.VULNERABILITY_ISSUE,
            ComplexityFactor.VULNERABILITY_ISSUE,
          )
        )

      val result = communityApiService.getRegistrations(crn).complexityFactors

      assertThat(result.size).isEqualTo(1)
    }

    @Test
    fun `should not count complexity factors none`() = runBlocking {
      coEvery { communityApiClient.getRegistrations(crn) } returns listOf()
      val result = communityApiService.getRegistrations(crn).complexityFactors

      assertThat(result.size).isEqualTo(0)
    }

    @Test
    fun `Should return Complexity Factor Case Insensitive`() = runBlocking {
      val registrations =
        listOf(
          Registration(
            KeyValue("rmdo"),
            KeyValue("Not Used"),

            LocalDate.now()
          )
        )
      coEvery { communityApiClient.getRegistrations(crn) } returns registrations
      val result = communityApiService.getRegistrations(crn).complexityFactors

      assertThat(result.size).isEqualTo(1)
    }

    @Test
    fun `Should return Complexity Factor if present as first value in list`() = runBlocking {
      val registrations =
        listOf(
          Registration(
            KeyValue("RMDO"),
            KeyValue("Not Used"),

            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S"),
            KeyValue("BD"),

            LocalDate.now()
          ),
          Registration(
            KeyValue("AV2S"),
            KeyValue("12"),

            LocalDate.now()
          ),

        )
      coEvery { communityApiClient.getRegistrations(crn) } returns registrations
      val result = communityApiService.getRegistrations(crn).complexityFactors
      assertThat(result.size).isEqualTo(1)
    }

    @Test
    fun `Should return empty List if no Complexity Factors present`() = runBlocking {

      val registrations =
        listOf(
          Registration(
            KeyValue("AV2S"),
            KeyValue("Not Used"),

            LocalDate.now()
          )
        )
      coEvery { communityApiClient.getRegistrations(crn) } returns registrations
      val result = communityApiService.getRegistrations(crn).complexityFactors
      assertThat(result.size).isEqualTo(0)
    }

    private fun getValidRegistrations(factors: List<ComplexityFactor>): Collection<Registration> {
      return factors.map {
        Registration(type = KeyValue(it.registerCode), registerLevel = null, startDate = LocalDate.now())
      }
    }
  }
}
