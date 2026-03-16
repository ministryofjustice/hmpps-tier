package uk.gov.justice.digital.hmpps.hmppstier.service

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierSummaryRepository
import uk.gov.justice.digital.hmpps.hmppstier.test.TestData
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
internal class TierV3ReaderTest {

    @Mock
    internal lateinit var tierSummaryRepository: TierSummaryRepository

    @Mock
    internal lateinit var tierCalculationRepository: TierCalculationRepository

    @Mock
    internal lateinit var tierCalculationService: TierCalculationService

    @Mock
    internal lateinit var tierUpdater: TierUpdater

    private lateinit var tierReader: TierV3Reader

    @BeforeEach
    fun setUp() {
        tierReader = TierV3Reader(tierCalculationRepository, tierSummaryRepository, tierCalculationService, tierUpdater)
    }

    @Test
    fun `where summary doesn't exist latest calculation is returned`() {
        val tierCalculation = tierCalculation(crn = TestData.crn(), tier = Tier.C)
        whenever(tierSummaryRepository.findById(tierCalculation.crn)).thenReturn(Optional.empty())
        whenever(tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(tierCalculation.crn)).thenReturn(
            tierCalculation
        )

        val result = tierReader.getLatestTierByCrn(tierCalculation.crn)

        assertThat(result?.tierScore, equalTo("C"))
        verify(tierUpdater).createSummary(tierCalculation)
    }

    @Test
    fun `tier is recalculated on the fly`() {
        val crn = TestData.crn()
        val tierCalculation = tierCalculation(crn = crn, tier = Tier.B)
        whenever(tierSummaryRepository.findById(crn)).thenReturn(Optional.empty())
        whenever(tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn)).thenReturn(null)
        whenever(tierCalculationService.calculateTierForCrn(eq(crn), any())).thenReturn(tierCalculation)

        val result = tierReader.getLatestTierByCrn(crn)

        assertThat(result?.calculationId, equalTo(tierCalculation.uuid))
        assertThat(result?.tierScore, equalTo("B"))
        verifyNoInteractions(tierUpdater)
    }

    @Test
    fun `tier counts are returned keyed by tier enum`() {
        whenever(tierSummaryRepository.getTierV3Counts()).thenReturn(listOf("A" to 12, "C" to 3))

        val result = tierReader.getTierCounts()

        assertThat(result, equalTo(mapOf(Tier.A to 12, Tier.C to 3)))
    }

    private fun tierCalculation(crn: String, tier: Tier) = TierCalculationEntity(
        id = 1L,
        uuid = UUID.randomUUID(),
        crn = crn,
        created = LocalDateTime.of(2026, 1, 1, 12, 0),
        data = TierCalculationResultEntity(
            tier = tier,
            protect = TierLevel(ProtectLevel.C, 0, mapOf()),
            change = TierLevel(ChangeLevel.THREE, 0, mapOf()),
            calculationVersion = "3"
        )
    )
}
