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
import uk.gov.justice.digital.hmpps.hmppstier.client.delius.*
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

    @ParameterizedTest(name = "CSE={0}, mainOffenceExcluded={1}, additionalOffenceExcluded={2} map to excluded={3}")
    @MethodSource("sentencingAct2026ExclusionCases")
    fun `maps sentencing act 2026 exclusions from CSE registrations and offences`(
        hasChildSexualExploitation: Boolean,
        mainOffenceExcluded: Boolean,
        additionalOffenceExcluded: Boolean,
        expectedExcluded: Boolean,
    ) {
        val startDate = LocalDate.of(2025, 2, 20)
        val registrations = listOfNotNull(
            registration(DeliusRegistration.DOMESTIC_ABUSE),
            registration(DeliusRegistration.CHILD_SEXUAL_EXPLOITATION).takeIf { hasChildSexualExploitation },
        )
        stubDeliusResponse(
            deliusResponse(
                registrations = registrations,
                convictions = listOf(
                    conviction(
                        startDate = startDate,
                        mainOffence = offence(sentencingAct2026Exclusion = mainOffenceExcluded),
                        additionalOffences = listOf(
                            offence(),
                            offence(sentencingAct2026Exclusion = additionalOffenceExcluded),
                        ),
                    ),
                ),
            ),
        )

        val result = deliusApiService.getTierToDelius(crn)

        assertThat(result.latestSentencingAct2026ExclusionDate)
            .isEqualTo(if (expectedExcluded) startDate else null)
    }

    @ParameterizedTest(name = "isCustodial={0}, startDate={1}, latestReleaseDate={2} map to exclusion date {3}")
    @MethodSource("sentencingAct2026ExclusionDateCases")
    fun `uses release date for custodial exclusions and start date for non-custodial exclusions`(
        isCustodial: Boolean,
        startDate: LocalDate?,
        latestReleaseDate: LocalDate?,
        expectedExclusionDate: LocalDate?,
    ) {
        stubDeliusResponse(
            deliusResponse(
                latestReleaseDate = LocalDate.of(2026, 1, 1),
                convictions = listOf(
                    conviction(
                        startDate = startDate,
                        latestReleaseDate = latestReleaseDate,
                        isCustodial = isCustodial,
                        mainOffence = offence(sentencingAct2026Exclusion = true),
                    ),
                ),
            ),
        )

        val result = deliusApiService.getTierToDelius(crn)

        assertThat(result.latestSentencingAct2026ExclusionDate).isEqualTo(expectedExclusionDate)
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

        @JvmStatic
        fun sentencingAct2026ExclusionCases() = listOf(
            // CSE, Main Offence Excluded, Additional Offence Excluded
            Arguments.of(false, false, false, false),
            Arguments.of(true, false, false, true),
            Arguments.of(false, true, false, true),
            Arguments.of(false, false, true, true),
            Arguments.of(true, true, false, true),
            Arguments.of(true, false, true, true),
            Arguments.of(false, true, true, true),
            Arguments.of(true, true, true, true),
        )

        @JvmStatic
        fun sentencingAct2026ExclusionDateCases(): List<Arguments> {
            val startDate = LocalDate.of(2024, 1, 1)
            val latestReleaseDate = LocalDate.of(2025, 2, 20)
            return listOf(
                Arguments.of(false, startDate, latestReleaseDate, startDate),
                Arguments.of(false, startDate, null, startDate),
                Arguments.of(false, null, latestReleaseDate, null),
                Arguments.of(false, null, null, null),
                Arguments.of(true, startDate, latestReleaseDate, latestReleaseDate),
                Arguments.of(true, null, latestReleaseDate, latestReleaseDate),
                Arguments.of(true, startDate, null, null),
                Arguments.of(true, null, null, null),
            )
        }

        private fun conviction(
            terminationDate: LocalDate? = null,
            sentenceTypeCode: String = "OTHER",
            requirements: List<DeliusRequirement> = emptyList(),
            startDate: LocalDate? = null,
            latestReleaseDate: LocalDate? = null,
            isCustodial: Boolean = false,
            mainOffence: DeliusOffence = offence(),
            additionalOffences: List<DeliusOffence> = emptyList(),
        ) = DeliusConviction(
            startDate = startDate,
            terminationDate = terminationDate,
            latestReleaseDate = latestReleaseDate,
            isCustodial = isCustodial,
            sentenceTypeCode = sentenceTypeCode,
            requirements = requirements,
            mainOffence = mainOffence,
            additionalOffences = additionalOffences,
        )

        private fun offence(
            sentencingAct2026Exclusion: Boolean = false,
        ) = DeliusOffence(
            code = "00100",
            description = "Test offence",
            sentencingAct2026Exclusion = sentencingAct2026Exclusion,
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
            hasActiveEvent: Boolean = true,
        ) = DeliusResponse(
            gender = gender,
            registrations = registrations,
            convictions = convictions,
            rsrscore = rsrScore,
            ogrsscore = ogrsScore,
            previousEnforcementActivity = previousEnforcementActivity,
            latestReleaseDate = latestReleaseDate,
            hasActiveEvent = hasActiveEvent,
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