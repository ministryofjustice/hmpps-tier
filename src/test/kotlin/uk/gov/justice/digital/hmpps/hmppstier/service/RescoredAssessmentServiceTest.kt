package uk.gov.justice.digital.hmpps.hmppstier.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ScoreLevel
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ScoreType
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.RescoredAssessmentEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.RescoredAssessmentRepository
import uk.gov.justice.digital.hmpps.hmppstier.test.TestData
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
internal class RescoredAssessmentServiceTest {

    @Mock
    lateinit var rescoredAssessmentRepository: RescoredAssessmentRepository

    @InjectMocks
    lateinit var rescoredAssessmentService: RescoredAssessmentService

    @Test
    fun `maps rescored assessment data to ogrs4 predictors`() {
        val crn = TestData.crn()
        val entity = RescoredAssessmentEntity(
            id = 1,
            crn = crn,
            completedDate = LocalDate.of(2025, 3, 1),
            arpScore = 75.0,
            arpIsDynamic = true,
            arpBand = "High",
            csrpScore = 1.0,
            csrpIsDynamic = false,
            csrpBand = "Low",
            dcSrpScore = 0.0,
            dcSrpBand = "NA",
            iicSrpScore = 0.0,
            iicSrpBand = "NA",
        )
        whenever(rescoredAssessmentRepository.findFirstByCrnOrderByCompletedDateDesc(eq(crn))).thenReturn(entity)

        val result = rescoredAssessmentService.getByCrn(crn)

        assertThat(result).isNotNull
        assertThat(result!!.completedDate).isEqualTo(entity.completedDate.atStartOfDay())
        assertThat(result.outputVersion).isEqualTo("2")
        assertThat(result.output?.allReoffendingPredictor?.score).isEqualByComparingTo("75.0")
        assertThat(result.output?.allReoffendingPredictor?.staticOrDynamic).isEqualTo(ScoreType.DYNAMIC)
        assertThat(result.output?.allReoffendingPredictor?.band).isEqualTo(ScoreLevel.HIGH)
        assertThat(result.output?.combinedSeriousReoffendingPredictor?.score).isEqualByComparingTo("1.0")
        assertThat(result.output?.combinedSeriousReoffendingPredictor?.staticOrDynamic).isEqualTo(ScoreType.STATIC)
        assertThat(result.output?.combinedSeriousReoffendingPredictor?.band).isEqualTo(ScoreLevel.LOW)
        assertThat(result.output?.directContactSexualReoffendingPredictor?.score).isEqualByComparingTo("0.0")
        assertThat(result.output?.directContactSexualReoffendingPredictor?.band).isEqualTo(ScoreLevel.NOT_APPLICABLE)
        assertThat(result.output?.indirectImageContactSexualReoffendingPredictor?.score).isEqualByComparingTo("0.0")
        assertThat(result.output?.indirectImageContactSexualReoffendingPredictor?.band).isEqualTo(ScoreLevel.NOT_APPLICABLE)
    }

    @Test
    fun `returns null when there is no rescored assessment for the crn`() {
        val crn = TestData.crn()
        whenever(rescoredAssessmentRepository.findFirstByCrnOrderByCompletedDateDesc(eq(crn))).thenReturn(null)

        val result = rescoredAssessmentService.getByCrn(crn)

        assertThat(result).isNull()
    }
}
