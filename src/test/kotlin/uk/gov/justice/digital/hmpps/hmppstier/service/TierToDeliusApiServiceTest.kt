package uk.gov.justice.digital.hmpps.hmppstier.service

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusResponse
import java.math.BigDecimal

@ExtendWith(MockKExtension::class)
@DisplayName("Tier to Delius Api Service tests")
class TierToDeliusApiServiceTest {
  private val tierToDeliusApiClient: TierToDeliusApiClient = mockk(relaxUnitFun = true)
  private val tierToDeliusApiService = TierToDeliusApiService(tierToDeliusApiClient)

  private val crn = "X123456"

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
  fun `Should return tier to delius response`() {
    runBlocking {
      val tierToDeliusResponse = TierToDeliusResponse(
        "Male",
        "UD0",
        emptyList(),
        emptyList(),
        BigDecimal.TEN,
        2,
      )

      coEvery { tierToDeliusApiClient.getDeliusTier(crn) } returns tierToDeliusResponse

      val result = tierToDeliusApiService.getTierToDelius(crn).currentTier
      Assertions.assertThat(result).isEqualTo("UD0")
    }
  }

  @Test
  fun `Empty currentTier`() {
    runBlocking {
      val tierToDeliusResponse = TierToDeliusResponse(
        "Male",
        null,
        emptyList(),
        emptyList(),
        BigDecimal.TEN,
        2,
      )

      coEvery { tierToDeliusApiClient.getDeliusTier(crn) } returns tierToDeliusResponse

      val result = tierToDeliusApiService.getTierToDelius(crn).currentTier
      Assertions.assertThat(result).isNull()
    }
  }
}
