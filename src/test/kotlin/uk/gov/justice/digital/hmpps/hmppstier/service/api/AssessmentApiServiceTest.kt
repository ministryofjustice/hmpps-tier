package uk.gov.justice.digital.hmpps.hmppstier.service.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppstier.client.ArnsApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.OGRS4Predictors
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ScoreType
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.SexualOffenceDto
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.AllPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.StaticOrDynamicPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.VersionedStaticOrDynamicPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.service.RescoredAssessmentService
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
internal class AssessmentApiServiceTest {
    @Mock
    internal lateinit var arnsApiClient: ArnsApiClient

    @Mock
    internal lateinit var rescoredAssessmentService: RescoredAssessmentService

    @InjectMocks
    internal lateinit var assessmentApiService: AssessmentApiService

    @Test
    fun `builds OASys tier inputs from the latest ARNS predictors and sexual offence flag`() {
        val crn = "X123456"
        val olderPredictors = riskPredictors(LocalDate.of(2025, 3, 1), arp = "0.0", csrp = "0.0")
        val latestPredictors = riskPredictors(LocalDate.of(2025, 3, 2), arp = "75.0", csrp = "1.0")

        whenever(arnsApiClient.getRiskPredictors(crn)).thenReturn(listOf(olderPredictors, latestPredictors))
        whenever(arnsApiClient.getSexuallyMotivatedOffence(crn)).thenReturn(SexualOffenceDto(true))

        val result = assessmentApiService.getOASysTierInputs(crn)

        assertThat(result!!.predictors).isSameAs(latestPredictors.output)
        assertThat(result.everCommittedSexualOffence).isTrue()
        verify(rescoredAssessmentService, never()).getByCrn(crn)
    }

    @Test
    fun `falls back to rescored predictors and defaults missing sexual offence flag to false`() {
        val crn = "X123456"
        val rescoredPredictors = riskPredictors(LocalDate.of(2025, 3, 1), arp = "75.0", csrp = "1.0")

        whenever(arnsApiClient.getRiskPredictors(crn)).thenReturn(null)
        whenever(rescoredAssessmentService.getByCrn(crn)).thenReturn(rescoredPredictors)
        whenever(arnsApiClient.getSexuallyMotivatedOffence(crn)).thenReturn(null)

        val result = assessmentApiService.getOASysTierInputs(crn)

        assertThat(result!!.predictors).isSameAs(rescoredPredictors.output)
        assertThat(result.everCommittedSexualOffence).isFalse()
    }

    private fun riskPredictors(
        completedDate: LocalDate,
        arp: String,
        csrp: String,
    ) = OGRS4Predictors(
        completedDate = completedDate.atStartOfDay(),
        output = AllPredictorDto(
            allReoffendingPredictor = StaticOrDynamicPredictorDto(
                staticOrDynamic = ScoreType.DYNAMIC,
                score = BigDecimal(arp),
            ),
            combinedSeriousReoffendingPredictor = VersionedStaticOrDynamicPredictorDto(
                staticOrDynamic = ScoreType.DYNAMIC,
                score = BigDecimal(csrp),
            ),
        ),
    )
}
