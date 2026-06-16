package uk.gov.justice.digital.hmpps.hmppstier.flags

import io.flipt.client.FliptClient
import io.flipt.client.models.BooleanEvaluationResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class FeatureFlagsTest {

    @Mock
    lateinit var client: FliptClient

    @Test
    fun `flags are enabled when Flipt client is not configured`() {
        assertThat(FeatureFlags(null).enabled("some-flag")).isTrue()
    }

    @Test
    fun `flag checks delegate to Flipt using flag key as entity id`() {
        whenever(client.evaluateBoolean(eq("tier-v3-events"), eq("tier-v3-events"), eq(emptyMap())))
            .thenReturn(BooleanEvaluationResponse.builder().enabled(false).build())

        assertThat(FeatureFlags(client).v3EventsEnabled).isFalse()

        verify(client).evaluateBoolean("tier-v3-events", "tier-v3-events", emptyMap())
    }

    @Test
    fun `Flipt failures are wrapped with the flag key`() {
        val failure = IllegalStateException("Flipt unavailable")
        whenever(client.evaluateBoolean(eq("tier-v3-calculation"), eq("tier-v3-calculation"), eq(emptyMap())))
            .thenThrow(failure)

        val exception = assertThrows<FeatureFlags.FeatureFlagException> {
            FeatureFlags(client).v3CalculationEnabled
        }

        assertThat(exception.key).isEqualTo("tier-v3-calculation")
        assertThat(exception).hasMessage("Unable to retrieve 'tier-v3-calculation' flag")
        assertThat(exception).hasCause(failure)
    }
}
