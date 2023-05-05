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
import uk.gov.justice.digital.hmpps.hmppstier.client.KeyValue
import uk.gov.justice.digital.hmpps.hmppstier.client.Nsi
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusResponse
import uk.gov.justice.digital.hmpps.hmppstier.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.domain.Sentence
import java.math.BigDecimal
import java.time.LocalDate

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


  @Test
  fun `Should return Breach points if false if present and valid terminationDate on cutoff`() = runBlocking {

    val crn = "123"
    val convictionId = 54321L
    val terminationDate = LocalDate.now(clock).minusYears(1).minusDays(1)
    val sentence = Sentence(terminationDate, irrelevantSentenceType)
    val conviction = Conviction(convictionId, sentence)

    val result = additionalFactorsForWomen.calculate(

      listOf(conviction),
      null,
      true,
    )
    Assertions.assertThat(result).isEqualTo(0)
  }

  @Test
  fun `Should return Breach true if present and valid not terminated`() = runBlocking {
    val crn = "123"
    val convictionId = 54321L
    val sentence = Sentence(null, irrelevantSentenceType)
    val conviction = Conviction(convictionId, sentence)
    val breaches = listOf(Nsi(status = KeyValue("BRE08")))
    coEvery { communityApiClient.getBreachRecallNsis( convictionId) } returns breaches

    val result = additionalFactorsForWomen.calculate(

      listOf(conviction),
      null,
      true,
    )
    Assertions.assertThat(result).isEqualTo(2)
    coVerify { communityApiClient.getBreachRecallNsis( convictionId) }
  }

  @Test
  fun `Should return Breach true if multiple convictions, one valid`() = runBlocking {
    val crn = "123"
    val convictionId = 54321L
    val sentence = Sentence(null, irrelevantSentenceType)
    val conviction = Conviction(convictionId, sentence)
    val unrelatedConviction = Conviction(convictionId.plus(1), sentence)
    val unrelatedBreaches = listOf(Nsi(status = KeyValue("BRE99")))
    val breaches = listOf(Nsi(status = KeyValue("BRE08")))
    coEvery { communityApiClient.getBreachRecallNsis( convictionId.plus(1)) } returns unrelatedBreaches
    coEvery { communityApiClient.getBreachRecallNsis( convictionId) } returns breaches

    val result = additionalFactorsForWomen.calculate(

      listOf(conviction, unrelatedConviction),
      null,
      true,
    )
    Assertions.assertThat(result).isEqualTo(2)
    coVerify { communityApiClient.getBreachRecallNsis( convictionId) }
  }

  @Test
  fun `Should return Breach false if no conviction`() = runBlocking {
    val crn = "123"

    val result = additionalFactorsForWomen.calculate(

      false,
      null,
      true,
    )
    Assertions.assertThat(result).isEqualTo(0)
  }

  @Test
  fun `Should return Breach true if one conviction, multiple breaches, one valid`() = runBlocking {
    val crn = "123"
    val convictionId = 54321L
    val sentence = Sentence(null, irrelevantSentenceType)
    val conviction = Conviction(convictionId, sentence)
    val breaches = listOf(
      Nsi(status = KeyValue("BRE54")),
      Nsi(status = KeyValue("BRE08")),
    )
    coEvery { communityApiClient.getBreachRecallNsis( convictionId) } returns breaches

    val result = additionalFactorsForWomen.calculate(

      listOf(conviction),
      null,
      true,
    )
    Assertions.assertThat(result).isEqualTo(2)
    coVerify { communityApiClient.getBreachRecallNsis( convictionId) }
  }

  @Test
  fun `Should return Breach true if one conviction, multiple breaches, one valid case insensitive`() = runBlocking {
    val crn = "123"
    val convictionId = 54321L
    val sentence = Sentence(null, irrelevantSentenceType)
    val conviction = Conviction(convictionId, sentence)
    val breaches = listOf(
      Nsi(status = KeyValue("BRE54")),
      Nsi(status = KeyValue("bre08")),
    )
    coEvery { communityApiClient.getBreachRecallNsis( convictionId) } returns breaches

    val result = additionalFactorsForWomen.calculate(

      listOf(conviction),
      null,
      true,
    )
    Assertions.assertThat(result).isEqualTo(2)
    coVerify { communityApiClient.getBreachRecallNsis( convictionId) }
  }

  @Test
  fun `Should return Breach false if one conviction, multiple breaches, none valid`() = runBlocking {
    val crn = "123"
    val convictionId = 54321L
    val sentence = Sentence(null, irrelevantSentenceType)
    val conviction = Conviction(convictionId, sentence)
    val breaches = listOf(
      Nsi(status = KeyValue("BRE99")),
      Nsi(status = KeyValue("BRE99")),
    )
    coEvery { communityApiClient.getBreachRecallNsis( convictionId) } returns breaches

    val result = additionalFactorsForWomen.calculate(

      listOf(conviction),
      null,
      true,
    )
    Assertions.assertThat(result).isEqualTo(0)
    coVerify { communityApiClient.getBreachRecallNsis( convictionId) }
  }
}
