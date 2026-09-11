package uk.gov.justice.digital.hmpps.hmppstier.service.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppstier.client.ArnsApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.OGRS4Predictors
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ScoreType
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.SexualOffenceDto
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.AllPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.StaticOrDynamicPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.VersionedStaticOrDynamicPredictorDto
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
internal class AssessmentApiServiceTest {
    @Mock
    internal lateinit var arnsApiClient: ArnsApiClient

    @InjectMocks
    internal lateinit var assessmentApiService: AssessmentApiService

    @Test
    fun `builds OASys tier inputs from the latest ARNS predictors and sexual offence flag`() {
        val crn = "X123456"
        val predictors = OGRS4Predictors(
            completedDate = LocalDate.of(2025, 3, 2).atStartOfDay(),
            output = AllPredictorDto(
                allReoffendingPredictor = StaticOrDynamicPredictorDto(
                    staticOrDynamic = ScoreType.DYNAMIC,
                    score = BigDecimal("75.0"),
                ),
                combinedSeriousReoffendingPredictor = VersionedStaticOrDynamicPredictorDto(
                    staticOrDynamic = ScoreType.DYNAMIC,
                    score = BigDecimal("1.0"),
                ),
            ),
        )

        whenever(arnsApiClient.getTierRiskPredictors(crn)).thenReturn(predictors)
        whenever(arnsApiClient.getSexuallyMotivatedOffence(crn)).thenReturn(SexualOffenceDto(true))

        val result = assessmentApiService.getOASysTierInputs(crn)

        assertThat(result!!.predictors).isSameAs(predictors)
        assertThat(result.everCommittedSexualOffence).isTrue()
    }
}
