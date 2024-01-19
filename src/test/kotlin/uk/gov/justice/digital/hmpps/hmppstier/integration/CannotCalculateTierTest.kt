package uk.gov.justice.digital.hmpps.hmppstier.integration

import io.hypersistence.utils.hibernate.type.util.ObjectMapperWrapper
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.tierToDeliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase

class CannotCalculateTierTest : IntegrationTestBase() {

    @Test
    fun `Offender does not exist`() {
        val crn = "X123456"
        tierToDeliusApi.getNotFound(crn)
        calculateTierFor(crn)

        verify(telemetryClient, timeout(2000)).trackEvent(
            "TierCalculationFailed",
            mapOf(
                "crn" to "X123456",
                "exception" to "404 Not Found from GET http://localhost:8093/tier-details/X123456",
                "recalculationReason" to "OffenderEventRecalculation",
            ),
            null,
        )
    }

    @Test
    fun `object mapper for hibernate should be the same as spring boot`() {
        assertThat(ObjectMapperWrapper.INSTANCE.objectMapper, equalTo(objectMapper))
    }
}
