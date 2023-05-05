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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusConviction
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusResponse
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
}
