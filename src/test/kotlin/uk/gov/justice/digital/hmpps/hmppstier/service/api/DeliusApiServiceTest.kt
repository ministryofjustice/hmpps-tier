package uk.gov.justice.digital.hmpps.hmppstier.service.api

import io.mockk.*
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.hmppstier.client.DeliusApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusConviction
import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusRegistration
import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusRequirement
import uk.gov.justice.digital.hmpps.hmppstier.client.delius.DeliusResponse
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.*
import uk.gov.justice.digital.hmpps.hmppstier.test.TestData
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
@DisplayName("Tier to Delius Api Service tests")
class DeliusApiServiceTest {
    private val deliusApiClient: DeliusApiClient = mockk(relaxUnitFun = true)
    private val deliusApiService = DeliusApiService(deliusApiClient)
    private val crn = TestData.crn()

    @BeforeEach
    fun resetAllMocks() {
        clearMocks(deliusApiClient)
    }

    @AfterEach
    fun confirmVerified() {
        coVerify(exactly = 1) { deliusApiClient.getDeliusTierInputs(crn) }
        confirmVerified(deliusApiClient)
    }

    @Test
    fun `maps top-level fields from API response`() {
        val latestReleaseDate = LocalDate.of(2025, 2, 20)
        stubDeliusResponse(
            deliusResponse(
                gender = "fEmAlE",
                rsrScore = BigDecimal("7.5"),
                ogrsScore = 42,
                previousEnforcementActivity = true,
                latestReleaseDate = latestReleaseDate,
            ),
        )

        val result = deliusApiService.getTierToDelius(crn)

        assertThat(result.isFemale).isTrue()
        assertThat(result.rsrScore).isEqualByComparingTo(BigDecimal("7.5"))
        assertThat(result.ogrsScore).isEqualTo(42)
        assertThat(result.previousEnforcementActivity).isTrue()
        assertThat(result.latestReleaseDate).isEqualTo(latestReleaseDate)
    }

    @Test
    fun `defaults null risk scores to zero`() {
        stubDeliusResponse(deliusResponse(rsrScore = null, ogrsScore = null))

        val result = deliusApiService.getTierToDelius(crn)

        assertThat(result.rsrScore).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(result.ogrsScore).isZero()
    }

    @ParameterizedTest(name = "convictions case {index} maps hasNoMandate={1}")
    @MethodSource("hasNoMandateCases")
    fun `maps hasNoMandate from convictions`(convictions: List<DeliusConviction>, expectedHasNoMandate: Boolean) {
        stubDeliusResponse(deliusResponse(convictions = convictions))

        val result = deliusApiService.getTierToDelius(crn)

        assertThat(result.hasNoMandate).isEqualTo(expectedHasNoMandate)
    }

    @Test
    fun `maps registration flags from registration codes`() {
        stubDeliusResponse(
            deliusResponse(
                registrations = listOf(
                    registration(IomNominal.IOM_NOMINAL.registerCode),
                    registration(DeliusRegistration.LIFER),
                    registration(DeliusRegistration.DOMESTIC_ABUSE),
                    registration(DeliusRegistration.STALKING),
                    registration(DeliusRegistration.CHILD_PROTECTION),
                    registration(DeliusRegistration.TWO_THIRDS_CODE),
                ),
            ),
        )

        val result = deliusApiService.getTierToDelius(crn).registrations

        assertThat(result.hasIomNominal).isTrue()
        assertThat(result.hasLiferIpp).isTrue()
        assertThat(result.hasDomesticAbuse).isTrue()
        assertThat(result.hasStalking).isTrue()
        assertThat(result.hasChildProtection).isTrue()
        assertThat(result.unsupervised).isTrue()
    }

    @Test
    fun `maps domestic abuse history code to domestic abuse flag`() {
        stubDeliusResponse(
            deliusResponse(
                registrations = listOf(
                    registration(DeliusRegistration.DOMESTIC_ABUSE_HISTORY),
                ),
            ),
        )

        val result = deliusApiService.getTierToDelius(crn).registrations

        assertThat(result.hasDomesticAbuse).isTrue()
    }

    @Test
    fun `maps convictions to hasActiveEvent flag`() {
        stubDeliusResponse(deliusResponse(convictions = listOf(conviction())))

        val result = deliusApiService.getTierToDelius(crn)

        assertThat(result.hasActiveEvent).isTrue()
    }

    @Test
    fun `maps convictions to hasActiveEvent flag ignoring terminated sentences`() {
        stubDeliusResponse(
            deliusResponse(
                convictions = listOf(
                    conviction(
                        terminationDate = LocalDate.now().minusDays(1)
                    )
                )
            )
        )

        val result = deliusApiService.getTierToDelius(crn)

        assertThat(result.hasActiveEvent).isFalse()
    }

    @Test
    fun `selects most recent rosh and ignores HREG`() {
        stubDeliusResponse(
            deliusResponse(
                registrations = listOf(
                    registration("RVHR", date = LocalDate.of(2024, 1, 1)),
                    registration("RMRH", date = LocalDate.of(2024, 5, 1)),
                    registration("HREG", date = LocalDate.of(2024, 12, 1)),
                ),
            ),
        )

        val result = deliusApiService.getTierToDelius(crn).registrations

        assertThat(result.rosh).isEqualTo(Rosh.MEDIUM)
    }

    @Test
    fun `selects most recent mappa level and category`() {
        stubDeliusResponse(
            deliusResponse(
                registrations = listOf(
                    registration(
                        code = DeliusRegistration.MAPPA,
                        level = "M1",
                        category = "M1",
                        date = LocalDate.of(2024, 1, 1),
                    ),
                    registration(
                        code = DeliusRegistration.MAPPA,
                        level = "M3",
                        category = "M2",
                        date = LocalDate.of(2024, 3, 1),
                    ),
                ),
            ),
        )

        val result = deliusApiService.getTierToDelius(crn).registrations

        assertThat(result.mappaLevel).isEqualTo(MappaLevel.M3)
        assertThat(result.mappaCategory).isEqualTo(MappaCategory.M2)
    }

    @Test
    fun `deduplicates complexity factors and matches codes case-insensitively`() {
        stubDeliusResponse(
            deliusResponse(
                registrations = listOf(
                    registration("RMDO"),
                    registration("rmdo"),
                    registration("RVLN"),
                    registration("AV2S"),
                ),
            ),
        )

        val result = deliusApiService.getTierToDelius(crn).registrations

        assertThat(result.complexityFactors).containsExactlyInAnyOrder(
            ComplexityFactor.MENTAL_HEALTH,
            ComplexityFactor.VULNERABILITY_ISSUE,
        )
    }

    private fun stubDeliusResponse(response: DeliusResponse) {
        coEvery { deliusApiClient.getDeliusTierInputs(crn) } returns response
    }

    companion object {
        @JvmStatic
        fun hasNoMandateCases() = listOf(
            Arguments.of(listOf(conviction()), true),
            Arguments.of(listOf(conviction(sentenceTypeCode = "NC")), false),
            Arguments.of(listOf(conviction(requirements = listOf(requirement(restrictive = false)))), false),
            Arguments.of(
                listOf(conviction(terminationDate = LocalDate.of(2024, 1, 1), sentenceTypeCode = "NC")),
                true
            ),
        )

        private fun conviction(
            terminationDate: LocalDate? = null,
            sentenceTypeCode: String = "OTHER",
            requirements: List<DeliusRequirement> = emptyList(),
        ) = DeliusConviction(
            terminationDate = terminationDate,
            sentenceTypeCode = sentenceTypeCode,
            requirements = requirements,
        )

        private fun requirement(
            mainCategoryTypeCode: String = "A",
            restrictive: Boolean = true,
        ) = DeliusRequirement(
            mainCategoryTypeCode = mainCategoryTypeCode,
            restrictive = restrictive,
        )

        private fun deliusResponse(
            gender: String = "Male",
            registrations: List<DeliusRegistration> = emptyList(),
            convictions: List<DeliusConviction> = listOf(conviction()),
            rsrScore: BigDecimal? = BigDecimal.TEN,
            ogrsScore: Int? = 2,
            previousEnforcementActivity: Boolean = false,
            latestReleaseDate: LocalDate? = null,
        ) = DeliusResponse(
            gender = gender,
            registrations = registrations,
            convictions = convictions,
            rsrscore = rsrScore,
            ogrsscore = ogrsScore,
            previousEnforcementActivity = previousEnforcementActivity,
            latestReleaseDate = latestReleaseDate,
        )

        private fun registration(
            code: String,
            level: String? = null,
            category: String? = null,
            date: LocalDate = LocalDate.of(2025, 1, 1),
        ) = DeliusRegistration(
            code = code,
            level = level,
            category = category,
            date = date,
        )
    }
}