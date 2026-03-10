package uk.gov.justice.digital.hmpps.hmppstier.service

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusConviction
import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusRegistration
import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusResponse
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.service.api.DeliusApiService
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
@DisplayName("Tier to Delius Api Service tests")
class DeliusApiServiceTest {
    private val deliusApiClient: DeliusApiClient = mockk(relaxUnitFun = true)
    private val deliusApiService = DeliusApiService(deliusApiClient)

    private val crn = "X123456"
    private val sentenceTypeCode = "irrelevantSentenceType"

    @BeforeEach
    fun resetAllMocks() {
        clearMocks(deliusApiClient)
    }

    @AfterEach
    fun confirmVerified() {
        coVerify { deliusApiClient.getDeliusTier(crn) }
        // Check we don't add any more calls without updating the tests
        io.mockk.confirmVerified(deliusApiClient)
    }

    @Test
    fun `Previous Enforcement Activity Should be true if API response is true`() {
        val deliusResponse = DeliusResponse(
            "Male",
            emptyList(),
            listOf(DeliusConviction(null, sentenceTypeCode, emptyList())),
            BigDecimal.TEN,
            2,
            true,
        )

        coEvery { deliusApiClient.getDeliusTier(crn) } returns deliusResponse
        val result = deliusApiService.getTierToDelius(crn)
        Assertions.assertTrue(result.previousEnforcementActivity)
    }

    @Test
    fun `ogrs score and rsr should be zero when null`() {
        val deliusResponse = DeliusResponse(
            "Male",
            emptyList(),
            listOf(DeliusConviction(null, sentenceTypeCode, emptyList())),
            null,
            null,
            true,
        )

        coEvery { deliusApiClient.getDeliusTier(crn) } returns deliusResponse
        val result = deliusApiService.getTierToDelius(crn)
        Assertions.assertTrue(result.previousEnforcementActivity)
        Assertions.assertTrue(result.rsrScore == BigDecimal.ZERO)
        Assertions.assertTrue(result.ogrsScore == 0)
    }

    @Nested
    @DisplayName("Simple ROSH tests")
    inner class SimpleRoshTests {

        @Test
        fun `should return null for No Rosh`() {
            val deliusResponse = DeliusResponse(
                "Male",
                emptyList(),
                emptyList(),
                BigDecimal.TEN,
                2,
                false,
            )

            coEvery { deliusApiClient.getDeliusTier(crn) } returns deliusResponse
            val result = deliusApiService.getTierToDelius(crn).registrations.rosh

            Assertions.assertNull(result)
        }

        @Test
        fun `Should return RoSH level if present`() {
            val deliusResponse = DeliusResponse(
                "Male",
                listOf(DeliusRegistration("RMRH", null, LocalDate.now())),
                emptyList(),
                BigDecimal.TEN,
                2,
                false,
            )

            coEvery { deliusApiClient.getDeliusTier(crn) } returns deliusResponse
            val result = deliusApiService.getTierToDelius(crn).registrations.rosh

            Assertions.assertEquals(Rosh.MEDIUM, result)
        }

        @Test
        fun `Should return RoSH level Case Insensitive`() {
            val deliusResponse = DeliusResponse(
                "Male",
                listOf(DeliusRegistration("rmrh", null, LocalDate.now())),
                emptyList(),
                BigDecimal.TEN,
                2,
                false,
            )

            coEvery { deliusApiClient.getDeliusTier(crn) } returns deliusResponse
            val result = deliusApiService.getTierToDelius(crn).registrations.rosh

            Assertions.assertEquals(Rosh.MEDIUM, result)
        }

        @Test
        fun `Should return RoSH level if present as first value in list`() {
            val deliusResponse = DeliusResponse(
                "Male",
                listOf(
                    DeliusRegistration("RMRH", null, LocalDate.now()),
                    DeliusRegistration("AV2S", null, LocalDate.now()),
                    DeliusRegistration("AV2S", null, LocalDate.now()),
                ),
                emptyList(),
                BigDecimal.TEN,
                2,
                false,
            )

            coEvery { deliusApiClient.getDeliusTier(crn) } returns deliusResponse
            val result = deliusApiService.getTierToDelius(crn).registrations.rosh

            Assertions.assertEquals(Rosh.MEDIUM, result)
        }

        @Test
        fun `Should return RoSH level if present as middle value in list`() {
            val deliusResponse = DeliusResponse(
                "Male",
                listOf(
                    DeliusRegistration("AV2S", null, LocalDate.now()),
                    DeliusRegistration("RMRH", null, LocalDate.now()),
                    DeliusRegistration("AV2S", null, LocalDate.now()),
                ),
                emptyList(),
                BigDecimal.TEN,
                2,
                false,
            )

            coEvery { deliusApiClient.getDeliusTier(crn) } returns deliusResponse
            val result = deliusApiService.getTierToDelius(crn).registrations.rosh

            Assertions.assertEquals(Rosh.MEDIUM, result)
        }
    }

    @Nested
    @DisplayName("Simple Mappa tests")
    inner class SimpleMappaTests {

        @Test
        fun `should return null for No Mappa`() {
            val deliusResponse = DeliusResponse(
                "Male",
                emptyList(),
                emptyList(),
                BigDecimal.TEN,
                2,
                false,
            )

            coEvery { deliusApiClient.getDeliusTier(crn) } returns deliusResponse
            val result = deliusApiService.getTierToDelius(crn).registrations.mappa

            Assertions.assertNull(result)
        }

        @Test
        fun `Should return Mappa level if present`() {
            val deliusResponse = DeliusResponse(
                "Male",
                listOf(DeliusRegistration("MAPP", "M3", LocalDate.now())),
                emptyList(),
                BigDecimal.TEN,
                2,
                false,
            )

            coEvery { deliusApiClient.getDeliusTier(crn) } returns deliusResponse
            val result = deliusApiService.getTierToDelius(crn).registrations.mappa

            Assertions.assertEquals(Mappa.M3, result)
        }

        @Test
        fun `Should return Mappa level Case Insensitive`() {
            val deliusResponse = DeliusResponse(
                "Male",
                listOf(DeliusRegistration("MAPP", "m3", LocalDate.now())),
                emptyList(),
                BigDecimal.TEN,
                2,
                false,
            )

            coEvery { deliusApiClient.getDeliusTier(crn) } returns deliusResponse
            val result = deliusApiService.getTierToDelius(crn).registrations.mappa

            Assertions.assertEquals(Mappa.M3, result)
        }

        @Test
        fun `Should return Mappa level if present as first value in list`() {
            val deliusResponse = DeliusResponse(
                "Male",
                listOf(
                    DeliusRegistration("MAPP", "M3", LocalDate.now()),
                    DeliusRegistration("AV2S", "BD", LocalDate.now()),
                    DeliusRegistration("AV2S", "12", LocalDate.now()),

                    ),
                emptyList(),
                BigDecimal.TEN,
                2,
                false,
            )

            coEvery { deliusApiClient.getDeliusTier(crn) } returns deliusResponse
            val result = deliusApiService.getTierToDelius(crn).registrations.mappa

            Assertions.assertEquals(Mappa.M3, result)
        }

        @Test
        fun `Should return null for invalid Mappa code`() {
            val deliusResponse = DeliusResponse(
                "Male",
                listOf(

                    DeliusRegistration("AV2S", "Not Used", LocalDate.now()),
                    DeliusRegistration("AV2S", "Not Used", LocalDate.now()),
                    DeliusRegistration("Not Used", "INVALID", LocalDate.now()),
                ),
                emptyList(),
                BigDecimal.TEN,
                2,
                false,
            )

            coEvery { deliusApiClient.getDeliusTier(crn) } returns deliusResponse
            val result = deliusApiService.getTierToDelius(crn).registrations.mappa
            Assertions.assertNull(result)
        }
    }

    @Nested
    @DisplayName("Simple Complexity tests")
    inner class SimpleComplexityTests {

        @Test
        fun `should count complexity factors `() {
            val deliusResponse = DeliusResponse(
                "Male",
                getValidRegistrations(
                    listOf(
                        ComplexityFactor.VULNERABILITY_ISSUE,
                        ComplexityFactor.ADULT_AT_RISK,
                    ),
                ),
                emptyList(),
                BigDecimal.TEN,
                2,
                false,
            )

            coEvery { deliusApiClient.getDeliusTier(crn) } returns deliusResponse
            val result = deliusApiService.getTierToDelius(crn).registrations.complexityFactors
            Assertions.assertEquals(2, result.size)
        }

        @Test
        fun `should not count complexity factors duplicates`() {
            val deliusResponse = DeliusResponse(
                "Male",
                getValidRegistrations(
                    listOf(
                        ComplexityFactor.VULNERABILITY_ISSUE,
                        ComplexityFactor.VULNERABILITY_ISSUE,
                    ),
                ),
                emptyList(),
                BigDecimal.TEN,
                2,
                false,
            )

            coEvery { deliusApiClient.getDeliusTier(crn) } returns deliusResponse
            val result = deliusApiService.getTierToDelius(crn).registrations.complexityFactors
            Assertions.assertEquals(1, result.size)
        }

        @Test
        fun `should not count complexity factors none`() {
            val deliusResponse = DeliusResponse(
                "Male",
                emptyList(),
                emptyList(),
                BigDecimal.TEN,
                2,
                false,
            )

            coEvery { deliusApiClient.getDeliusTier(crn) } returns deliusResponse
            val result = deliusApiService.getTierToDelius(crn).registrations.complexityFactors
            Assertions.assertEquals(0, result.size)
        }

        @Test
        fun `Should return Complexity Factor Case Insensitive`() {
            val deliusResponse = DeliusResponse(
                "Male",
                listOf(
                    DeliusRegistration("rmdo", "Not Used", LocalDate.now()),
                ),
                emptyList(),
                BigDecimal.TEN,
                2,
                false,
            )

            coEvery { deliusApiClient.getDeliusTier(crn) } returns deliusResponse
            val result = deliusApiService.getTierToDelius(crn).registrations.complexityFactors
            Assertions.assertEquals(1, result.size)
        }

        @Test
        fun `Should return Complexity Factor if present as first value in list`() {
            val deliusResponse = DeliusResponse(
                "Male",
                listOf(
                    DeliusRegistration("RMDO", "Not Used", LocalDate.now()),
                    DeliusRegistration("AV2S", "BD", LocalDate.now()),
                    DeliusRegistration("AV2S", "12", LocalDate.now()),
                ),
                emptyList(),
                BigDecimal.TEN,
                2,
                false,
            )

            coEvery { deliusApiClient.getDeliusTier(crn) } returns deliusResponse
            val result = deliusApiService.getTierToDelius(crn).registrations.complexityFactors
            Assertions.assertEquals(1, result.size)
        }

        @Test
        fun `Should return empty List if no Complexity Factors present`() {
            val deliusResponse = DeliusResponse(
                "Male",
                listOf(
                    DeliusRegistration("AV2S", "Not Used", LocalDate.now()),
                ),
                emptyList(),
                BigDecimal.TEN,
                2,
                false,
            )

            coEvery { deliusApiClient.getDeliusTier(crn) } returns deliusResponse
            val result = deliusApiService.getTierToDelius(crn).registrations.complexityFactors
            Assertions.assertEquals(0, result.size)
        }

        private fun getValidRegistrations(factors: List<ComplexityFactor>): List<DeliusRegistration> {
            return factors.map {
                DeliusRegistration(it.registerCode, null, LocalDate.now())
            }
        }
    }
}
