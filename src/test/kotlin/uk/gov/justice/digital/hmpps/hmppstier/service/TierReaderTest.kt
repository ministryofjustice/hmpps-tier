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
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierSummary
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierSummaryRepository
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
internal class TierReaderTest {

    @Mock
    internal lateinit var tierSummaryRepository: TierSummaryRepository

    @Mock
    internal lateinit var tierCalculationRepository: TierCalculationRepository

    @Mock
    internal lateinit var tierCalculationService: TierCalculationService

    internal lateinit var tierReader: TierReader

    @Test
    fun `where summary doesn't exist detail is returned`() {
        tierReader = TierReader(tierCalculationRepository, tierSummaryRepository, tierCalculationService, true)
        val tierCalculation = TierCalculationEntity(
            1L,
            UUID.randomUUID(),
            "N123567",
            LocalDateTime.now(),
            TierCalculationResultEntity(
                TierLevel(ProtectLevel.C, 15, mapOf()),
                TierLevel(ChangeLevel.THREE, 12, mapOf()),
                "1",
                deliusInputs = DeliusInputs(
                    false,
                    BigDecimal.TEN,
                    33,
                    false,
                    Registrations(false, listOf(), null, null, true),
                    false
                )
            )
        )
        whenever(tierSummaryRepository.findById(tierCalculation.crn)).thenReturn(Optional.empty())
        whenever(tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(tierCalculation.crn))
            .thenReturn(tierCalculation)

        val res = tierReader.getLatestTierByCrn(tierCalculation.crn)
        assertThat(res?.tierScore, equalTo("C3S"))

        verify(tierSummaryRepository).save(any())
    }

    @Test
    fun `tier is recalculated on the fly`() {
        tierReader = TierReader(tierCalculationRepository, tierSummaryRepository, tierCalculationService, true)
        val tierCalculation = mock<TierCalculationEntity>(RETURNS_DEEP_STUBS)
        val crn = "N123567"
        whenever(tierSummaryRepository.findById(crn)).thenReturn(Optional.empty())
        whenever(tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn)).thenReturn(null)
        whenever(tierCalculationService.calculateTierForCrn(eq(crn), any(), any())).thenReturn(tierCalculation)

        val res = tierReader.getLatestTierByCrn(crn)
        assertThat(res?.calculationId, equalTo(tierCalculation.uuid))
    }

    @Test
    fun `when suffix deactivated no suffix is provided`() {
        tierReader = TierReader(tierCalculationRepository, tierSummaryRepository, tierCalculationService, false)
        val tierSummary = TierSummary(
            "S123456",
            UUID.randomUUID(),
            "C",
            3,
            true,
            0,
            LocalDateTime.now()
        )

        whenever(tierSummaryRepository.findById(tierSummary.crn)).thenReturn(Optional.of(tierSummary))

        val res = tierReader.getLatestTierByCrn(tierSummary.crn)
        assertThat(res?.tierScore, equalTo("C3"))
    }
}