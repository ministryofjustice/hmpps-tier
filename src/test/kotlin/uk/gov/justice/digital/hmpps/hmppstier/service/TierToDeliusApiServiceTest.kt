package uk.gov.justice.digital.hmpps.hmppstier.service

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusConviction
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusRegistration
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusResponse
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
@DisplayName("Tier to Delius Api Service tests")
class TierToDeliusApiServiceTest {
  private val tierToDeliusApiClient: TierToDeliusApiClient = mockk(relaxUnitFun = true)
  private val tierToDeliusApiService = TierToDeliusApiService(tierToDeliusApiClient)

  private val crn = "X123456"
  private val sentenceTypeCode = "irrelevantSentenceType"

  @BeforeEach
  fun resetAllMocks() {
    clearMocks(tierToDeliusApiClient)
  }

  @AfterEach
  fun confirmVerified() {
    coVerify { tierToDeliusApiClient.getDeliusTier(crn) }
    // Check we don't add any more calls without updating the tests
    io.mockk.confirmVerified(tierToDeliusApiClient)
  }

  @Test
  fun `Should return Breach points if false if present and valid terminationDate on cutoff`() = runBlocking {
    val terminationDate = LocalDate.now().minusYears(1).minusDays(1)

    val tierToDeliusResponse = TierToDeliusResponse(
      "Male",
      null,
      emptyList(),
      listOf(DeliusConviction(terminationDate, sentenceTypeCode, "description", true, emptyList())),
      BigDecimal.TEN,
      2,
    )

    coEvery { tierToDeliusApiClient.getDeliusTier(crn) } returns tierToDeliusResponse
    val result = tierToDeliusApiService.getTierToDelius(crn).breached

    Assertions.assertFalse(result)
  }

  @Test
  fun `Should return Breach true if present and valid not terminated`() = runBlocking {
    val tierToDeliusResponse = TierToDeliusResponse(
      "Male",
      null,
      emptyList(),
      listOf(DeliusConviction(null, sentenceTypeCode, "description", true, emptyList())),
      BigDecimal.TEN,
      2,
    )

    coEvery { tierToDeliusApiClient.getDeliusTier(crn) } returns tierToDeliusResponse
    val result = tierToDeliusApiService.getTierToDelius(crn).breached
    Assertions.assertTrue(result)
  }

  @Test
  fun `Should return Breach true if multiple convictions, one valid`() = runBlocking {
    val tierToDeliusResponse = TierToDeliusResponse(
      "Male",
      null,
      emptyList(),
      listOf(
        DeliusConviction(null, sentenceTypeCode, "description", true, emptyList()),
        DeliusConviction(null, sentenceTypeCode, "description", false, emptyList()),
      ),
      BigDecimal.TEN,
      2,
    )

    coEvery { tierToDeliusApiClient.getDeliusTier(crn) } returns tierToDeliusResponse
    val result = tierToDeliusApiService.getTierToDelius(crn).breached
    Assertions.assertTrue(result)
  }

  @Test
  fun `Should return Breach false if no conviction`() = runBlocking {
    val tierToDeliusResponse = TierToDeliusResponse(
      "Male",
      null,
      emptyList(),
      emptyList(),
      BigDecimal.TEN,
      2,
    )

    coEvery { tierToDeliusApiClient.getDeliusTier(crn) } returns tierToDeliusResponse
    val result = tierToDeliusApiService.getTierToDelius(crn).breached

    Assertions.assertFalse(result)
  }

  @Nested
  @DisplayName("Simple ROSH tests")
  inner class SimpleRoshTests {

    @Test
    fun `should return null for No Rosh`() = runBlocking {
      val tierToDeliusResponse = TierToDeliusResponse(
        "Male",
        null,
        emptyList(),
        emptyList(),
        BigDecimal.TEN,
        2,
      )

      coEvery { tierToDeliusApiClient.getDeliusTier(crn) } returns tierToDeliusResponse
      val result = tierToDeliusApiService.getTierToDelius(crn).registrations.rosh

      Assertions.assertNull(result)
    }

    @Test
    fun `Should return RoSH level if present`() = runBlocking {
      val tierToDeliusResponse = TierToDeliusResponse(
        "Male",
        null,
        listOf(DeliusRegistration("RMRH", "Description", null, LocalDate.now())),
        emptyList(),
        BigDecimal.TEN,
        2,
      )

      coEvery { tierToDeliusApiClient.getDeliusTier(crn) } returns tierToDeliusResponse
      val result = tierToDeliusApiService.getTierToDelius(crn).registrations.rosh

      Assertions.assertEquals(Rosh.MEDIUM, result)
    }

    @Test
    fun `Should return RoSH level Case Insensitive`() = runBlocking {
      val tierToDeliusResponse = TierToDeliusResponse(
        "Male",
        null,
        listOf(DeliusRegistration("rmrh", "Description", null, LocalDate.now())),
        emptyList(),
        BigDecimal.TEN,
        2,
      )

      coEvery { tierToDeliusApiClient.getDeliusTier(crn) } returns tierToDeliusResponse
      val result = tierToDeliusApiService.getTierToDelius(crn).registrations.rosh

      Assertions.assertEquals(Rosh.MEDIUM, result)
    }

    @Test
    fun `Should return RoSH level if present as first value in list`() = runBlocking {
      val tierToDeliusResponse = TierToDeliusResponse(
        "Male",
        null,
        listOf(
          DeliusRegistration("RMRH", "Description", null, LocalDate.now()),
          DeliusRegistration("AV2S", "Description", null, LocalDate.now()),
          DeliusRegistration("AV2S", "Description", null, LocalDate.now()),
        ),
        emptyList(),
        BigDecimal.TEN,
        2,
      )

      coEvery { tierToDeliusApiClient.getDeliusTier(crn) } returns tierToDeliusResponse
      val result = tierToDeliusApiService.getTierToDelius(crn).registrations.rosh

      Assertions.assertEquals(Rosh.MEDIUM, result)
    }

    @Test
    fun `Should return RoSH level if present as middle value in list`() = runBlocking {
      val tierToDeliusResponse = TierToDeliusResponse(
        "Male",
        null,
        listOf(
          DeliusRegistration("AV2S", "Description", null, LocalDate.now()),
          DeliusRegistration("RMRH", "Description", null, LocalDate.now()),
          DeliusRegistration("AV2S", "Description", null, LocalDate.now()),
        ),
        emptyList(),
        BigDecimal.TEN,
        2,
      )

      coEvery { tierToDeliusApiClient.getDeliusTier(crn) } returns tierToDeliusResponse
      val result = tierToDeliusApiService.getTierToDelius(crn).registrations.rosh

      Assertions.assertEquals(Rosh.MEDIUM, result)
    }
  }

  @Nested
  @DisplayName("Simple Mappa tests")
  inner class SimpleMappaTests {

    @Test
    fun `should return null for No Mappa`() = runBlocking {
      val tierToDeliusResponse = TierToDeliusResponse(
        "Male",
        null,
        emptyList(),
        emptyList(),
        BigDecimal.TEN,
        2,
      )

      coEvery { tierToDeliusApiClient.getDeliusTier(crn) } returns tierToDeliusResponse
      val result = tierToDeliusApiService.getTierToDelius(crn).registrations.mappa

      Assertions.assertNull(result)
    }

    @Test
    fun `Should return Mappa level if present`() = runBlocking {
      val tierToDeliusResponse = TierToDeliusResponse(
        "Male",
        null,
        listOf(DeliusRegistration("MAPP", "Description", "M3", LocalDate.now())),
        emptyList(),
        BigDecimal.TEN,
        2,
      )

      coEvery { tierToDeliusApiClient.getDeliusTier(crn) } returns tierToDeliusResponse
      val result = tierToDeliusApiService.getTierToDelius(crn).registrations.mappa

      Assertions.assertEquals(Mappa.M3, result)
    }

    @Test
    fun `Should return Mappa level Case Insensitive`() = runBlocking {
      val tierToDeliusResponse = TierToDeliusResponse(
        "Male",
        null,
        listOf(DeliusRegistration("MAPP", "Description", "m3", LocalDate.now())),
        emptyList(),
        BigDecimal.TEN,
        2,
      )

      coEvery { tierToDeliusApiClient.getDeliusTier(crn) } returns tierToDeliusResponse
      val result = tierToDeliusApiService.getTierToDelius(crn).registrations.mappa

      Assertions.assertEquals(Mappa.M3, result)
    }

    @Test
    fun `Should return Mappa level if present as first value in list`() = runBlocking {
      val tierToDeliusResponse = TierToDeliusResponse(
        "Male",
        null,
        listOf(
          DeliusRegistration("MAPP", "Description", "M3", LocalDate.now()),
          DeliusRegistration("AV2S", "Description", "BD", LocalDate.now()),
          DeliusRegistration("AV2S", "Description", "12", LocalDate.now()),

        ),
        emptyList(),
        BigDecimal.TEN,
        2,
      )

      coEvery { tierToDeliusApiClient.getDeliusTier(crn) } returns tierToDeliusResponse
      val result = tierToDeliusApiService.getTierToDelius(crn).registrations.mappa

      Assertions.assertEquals(Mappa.M3, result)
    }

    @Test
    fun `Should return null for invalid Mappa code`() = runBlocking {
      val tierToDeliusResponse = TierToDeliusResponse(
        "Male",
        null,
        listOf(

          DeliusRegistration("AV2S", "Description", "Not Used", LocalDate.now()),
          DeliusRegistration("AV2S", "Description", "Not Used", LocalDate.now()),
          DeliusRegistration("Not Used", "Description", "INVALID", LocalDate.now()),
        ),
        emptyList(),
        BigDecimal.TEN,
        2,
      )

      coEvery { tierToDeliusApiClient.getDeliusTier(crn) } returns tierToDeliusResponse
      val result = tierToDeliusApiService.getTierToDelius(crn).registrations.mappa
      Assertions.assertNull(result)
    }
  }

  @Nested
  @DisplayName("Simple Complexity tests")
  inner class SimpleComplexityTests {

    @Test
    fun `should count complexity factors `() = runBlocking {
      val tierToDeliusResponse = TierToDeliusResponse(
        "Male",
        null,
        getValidRegistrations(
          listOf(
            ComplexityFactor.VULNERABILITY_ISSUE,
            ComplexityFactor.ADULT_AT_RISK,
          ),
        ),
        emptyList(),
        BigDecimal.TEN,
        2,
      )

      coEvery { tierToDeliusApiClient.getDeliusTier(crn) } returns tierToDeliusResponse
      val result = tierToDeliusApiService.getTierToDelius(crn).registrations.complexityFactors
      Assertions.assertEquals(2, result.size)
    }

    @Test
    fun `should not count complexity factors duplicates`() = runBlocking {
      val tierToDeliusResponse = TierToDeliusResponse(
        "Male",
        null,
        getValidRegistrations(
          listOf(
            ComplexityFactor.VULNERABILITY_ISSUE,
            ComplexityFactor.VULNERABILITY_ISSUE,
          ),
        ),
        emptyList(),
        BigDecimal.TEN,
        2,
      )

      coEvery { tierToDeliusApiClient.getDeliusTier(crn) } returns tierToDeliusResponse
      val result = tierToDeliusApiService.getTierToDelius(crn).registrations.complexityFactors
      Assertions.assertEquals(1, result.size)
    }

    @Test
    fun `should not count complexity factors none`() = runBlocking {
      val tierToDeliusResponse = TierToDeliusResponse(
        "Male",
        null,
        emptyList(),
        emptyList(),
        BigDecimal.TEN,
        2,
      )

      coEvery { tierToDeliusApiClient.getDeliusTier(crn) } returns tierToDeliusResponse
      val result = tierToDeliusApiService.getTierToDelius(crn).registrations.complexityFactors
      Assertions.assertEquals(0, result.size)
    }

    @Test
    fun `Should return Complexity Factor Case Insensitive`() = runBlocking {
      val tierToDeliusResponse = TierToDeliusResponse(
        "Male",
        null,
        listOf(
          DeliusRegistration("rmdo", "Description", "Not Used", LocalDate.now()),
        ),
        emptyList(),
        BigDecimal.TEN,
        2,
      )

      coEvery { tierToDeliusApiClient.getDeliusTier(crn) } returns tierToDeliusResponse
      val result = tierToDeliusApiService.getTierToDelius(crn).registrations.complexityFactors
      Assertions.assertEquals(1, result.size)
    }

    @Test
    fun `Should return Complexity Factor if present as first value in list`() = runBlocking {
      val tierToDeliusResponse = TierToDeliusResponse(
        "Male",
        null,
        listOf(
          DeliusRegistration("RMDO", "Description", "Not Used", LocalDate.now()),
          DeliusRegistration("AV2S", "Description", "BD", LocalDate.now()),
          DeliusRegistration("AV2S", "Description", "12", LocalDate.now()),
        ),
        emptyList(),
        BigDecimal.TEN,
        2,
      )

      coEvery { tierToDeliusApiClient.getDeliusTier(crn) } returns tierToDeliusResponse
      val result = tierToDeliusApiService.getTierToDelius(crn).registrations.complexityFactors
      Assertions.assertEquals(1, result.size)
    }

    @Test
    fun `Should return empty List if no Complexity Factors present`() = runBlocking {
      val tierToDeliusResponse = TierToDeliusResponse(
        "Male",
        null,
        listOf(
          DeliusRegistration("AV2S", "Description", "Not Used", LocalDate.now()),
        ),
        emptyList(),
        BigDecimal.TEN,
        2,
      )

      coEvery { tierToDeliusApiClient.getDeliusTier(crn) } returns tierToDeliusResponse
      val result = tierToDeliusApiService.getTierToDelius(crn).registrations.complexityFactors
      Assertions.assertEquals(0, result.size)
    }

    private fun getValidRegistrations(factors: List<ComplexityFactor>): List<DeliusRegistration> {
      return factors.map {
        DeliusRegistration(it.registerCode, "Description", null, LocalDate.now())
      }
    }
  }
}
