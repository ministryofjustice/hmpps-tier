package uk.gov.justice.digital.hmpps.hmppstier.integration

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.web.reactive.function.BodyInserters.empty
import org.springframework.web.reactive.function.BodyInserters.fromValue
import uk.gov.justice.digital.hmpps.hmppstier.client.TierToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppstier.service.TriggerCalculationService

@OptIn(ExperimentalCoroutinesApi::class)
class TriggerRecalculationsTest : IntegrationTestBase() {
  @SpyBean
  internal lateinit var triggerCalculationService: TriggerCalculationService

  @MockBean
  internal lateinit var tierToDeliusApiClient: TierToDeliusApiClient

  @Test
  fun `providing crns recalculates only those crns`() = runTest {
    webTestClient.post()
      .uri("calculations")
      .body(fromValue(listOf("A123456", "B123456", "C123456")))
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk

    val crnCaptor = argumentCaptor<String>()
    verify(triggerCalculationService, times(3)).recalculate(crnCaptor.capture())
    assertThat(crnCaptor.firstValue, equalTo("A123456"))
    assertThat(crnCaptor.secondValue, equalTo("B123456"))
    assertThat(crnCaptor.thirdValue, equalTo("C123456"))
  }

  @Test
  fun `providing no crns recalculates all active crns from delius`() = runTest {
    whenever(tierToDeliusApiClient.getActiveCrns()).thenReturn(
      flow {
        emit("Z987654")
        emit("Y987654")
        emit("X987654")
      },
    )

    webTestClient.post()
      .uri("calculations")
      .body(empty<List<String>>())
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk

    verify(triggerCalculationService).recalculateAll()
  }
}
