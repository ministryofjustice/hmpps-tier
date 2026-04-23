package uk.gov.justice.digital.hmpps.hmppstier.service

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Answers.RETURNS_DEEP_STUBS
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusInputs
import uk.gov.justice.digital.hmpps.hmppstier.domain.Registrations
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierSummaryRepository
import uk.gov.justice.digital.hmpps.hmppstier.test.TestData
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
internal class TierV2ReaderTest {

    @Mock
    internal lateinit var tierSummaryRepository: TierSummaryRepository

    @Mock
    internal lateinit var tierCalculationRepository: TierCalculationRepository

    @Mock
    internal lateinit var tierCalculationService: TierCalculationService

    @Mock
    internal lateinit var tierUpdater: TierUpdater

    internal lateinit var tierReader: TierV2Reader

    @Test
    fun `where summary doesn't exist detail is returned`() {
        tierReader = TierV2Reader(tierCalculationRepository, tierSummaryRepository, tierCalculationService, tierUpdater)
        val tierCalculation = TierCalculationEntity(
            1L,
            UUID.randomUUID(),
            "N123567",
            LocalDateTime.now(),
            TierCalculationResultEntity(
                tier = Tier.C,
                protect = TierLevel(ProtectLevel.C, 15, mapOf()),
                change = TierLevel(ChangeLevel.THREE, 12, mapOf()),
                calculationVersion = "1",
                deliusInputs = DeliusInputs(
                    isFemale = false,
                    rsrScore = BigDecimal.TEN,
                    ogrsScore = 33,
                    hasNoMandate = false,
                    registrations = Registrations(
                        hasIomNominal = false,
                        hasLiferIpp = false,
                        hasDomesticAbuse = false,
                        hasStalking = false,
                        hasChildProtection = false,
                        complexityFactors = listOf(),
                        rosh = null,
                        mappaLevel = null,
                        mappaCategory = null,
                        unsupervised = true
                    ),
                    previousEnforcementActivity = false,
                    latestReleaseDate = null,
                    hasActiveEvent = false,
                )
            )
        )
        whenever(tierSummaryRepository.findById(tierCalculation.crn)).thenReturn(Optional.empty())
        whenever(tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(tierCalculation.crn))
            .thenReturn(tierCalculation)

        val res = tierReader.getLatestTierByCrn(tierCalculation.crn)
        assertThat(res?.tierScore, equalTo("C3S"))

        verify(tierUpdater).createSummary(tierCalculation)
    }

    @Test
    fun `tier is recalculated on the fly`() {
        tierReader = TierV2Reader(tierCalculationRepository, tierSummaryRepository, tierCalculationService, tierUpdater)
        val tierCalculation = mock<TierCalculationEntity>(RETURNS_DEEP_STUBS)
        val crn = TestData.crn()
        whenever(tierSummaryRepository.findById(crn)).thenReturn(Optional.empty())
        whenever(tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn)).thenReturn(null)
        whenever(tierCalculationService.calculateTierForCrn(eq(crn), any())).thenReturn(tierCalculation)

        val res = tierReader.getLatestTierByCrn(crn)
        assertThat(res?.calculationId, equalTo(tierCalculation.uuid))
    }
}