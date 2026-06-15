package uk.gov.justice.digital.hmpps.hmppstier.flags

import io.flipt.client.FliptClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FeatureFlags(private val client: FliptClient?) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    val v3CalculationEnabled get() = enabled("tier-v3-calculation")
    val v3EventsEnabled get() = enabled("tier-v3-events")

    fun enabled(key: String) = try {
        if (client == null) {
            log.warn("Flipt client not configured, all feature flags enabled.")
            true
        } else {
            client.evaluateBoolean(key, key, emptyMap<String, String>()).isEnabled
        }
    } catch (e: Exception) {
        throw FeatureFlagException(key, e)
    }

    class FeatureFlagException(val key: String, e: Exception) : RuntimeException("Unable to retrieve '$key' flag", e)
}
