package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase

class AuthorisationTest : IntegrationTestBase() {

    @ParameterizedTest
    @ValueSource(strings = ["/tier-counts", "/v2/tier-counts", "/v3/tier-counts"])
    fun `returns 401 when tier endpoint is called without a token`(endpoint: String) {
        mockMvc.perform(get(endpoint))
            .andExpect(status().isUnauthorized)
    }

    @ParameterizedTest
    @ValueSource(strings = ["/tier-counts", "/v2/tier-counts", "/v3/tier-counts"])
    fun `returns 403 when tier endpoint is called without hmpps tier role`(endpoint: String) {
        val auth = jwtHelper.setAuthorisationHeader(roles = listOf("SOME_OTHER_ROLE"))
        mockMvc.perform(get(endpoint).headers(auth))
            .andExpect(status().isForbidden)
    }
}
