package uk.gov.justice.digital.hmpps.hmppstier.service

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusInputs
import uk.gov.justice.digital.hmpps.hmppstier.domain.Registrations
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
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

    @InjectMocks
    internal lateinit var tierReader: TierReader

    @Test
    fun `where summary doesn't exist detail is returned`() {
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
}