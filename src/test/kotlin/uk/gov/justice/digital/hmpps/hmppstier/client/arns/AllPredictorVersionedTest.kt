package uk.gov.justice.digital.hmpps.hmppstier.client.arns

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

class AllPredictorVersionedTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `legacy predictor output deserialises as OGRS3 so callers can ignore it`() {
        val predictors = objectMapper.readValue<List<AllPredictorVersioned<Any>>>(
            """
            [
              {
                "completedDate": null,
                "assessmentType": null,
                "outputVersion": "1",
                "output": {
                  "legacyScore": 42
                }
              }
            ]
            """.trimIndent(),
        )

        assertThat(predictors.single()).isInstanceOf(OGRS3PRedictors::class.java)
        assertThat(predictors.single().outputVersion).isEqualTo("1")
    }
}
