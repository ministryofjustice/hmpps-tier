package uk.gov.justice.digital.hmpps.hmppstier.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.OGRS4Predictors
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ScoreType
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.AllPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.StaticOrDynamicPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.VersionedStaticOrDynamicPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.domain.DeliusInputs
import uk.gov.justice.digital.hmpps.hmppstier.domain.RecalculationSource
import uk.gov.justice.digital.hmpps.hmppstier.domain.Registrations
import uk.gov.justice.digital.hmpps.hmppstier.domain.TelemetryEventType
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Tier
import uk.gov.justice.digital.hmpps.hmppstier.flags.FeatureFlags
import uk.gov.justice.digital.hmpps.hmppstier.messaging.publisher.DomainEventPublisher
import uk.gov.justice.digital.hmpps.hmppstier.service.api.AssessmentApiService
import uk.gov.justice.digital.hmpps.hmppstier.service.api.DeliusApiService
import uk.gov.justice.digital.hmpps.hmppstier.test.TestData
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@ExtendWith(MockitoExtension::class)
internal class TierCalculationServiceTest {
    @Mock
    internal lateinit var clock: Clock

    @Mock
    internal lateinit var assessmentApiService: AssessmentApiService

    @Mock
    internal lateinit var deliusApiService: DeliusApiService

    @Mock
    internal lateinit var domainEventPublisher: DomainEventPublisher

    @Mock
    internal lateinit var telemetryService: TelemetryService

    @Mock
    internal lateinit var tierUpdater: TierUpdater

    @Mock
    internal lateinit var rescoredAssessmentService: RescoredAssessmentService

    @Mock
    internal lateinit var featureFlags: FeatureFlags

    @InjectMocks
    internal lateinit var tierCalculationService: TierCalculationService

    @Test
    fun `failure to remove tier logs to app insights`() {
        val crn = TestData.crn()
        val reason = "Events Terminated"
        val message = "Some issue with db"
        whenever(tierUpdater.removeTierCalculationsFor(crn)).thenThrow(RuntimeException(message))
        assertThrows<RuntimeException> { tierCalculationService.deleteCalculationsForCrn(crn, reason) }
        verify(telemetryService).trackEvent(
            TelemetryEventType.TIER_CALCULATION_REMOVAL_FAILED,
            mapOf("crn" to crn, "reasonToDelete" to reason, "failureReason" to message)
        )
    }

    @Test
    fun `uses rescored predictors when ARNS predictors are unavailable`() {
        whenever(featureFlags.v3Enabled).thenReturn(true)

        val crn = TestData.crn()
        val rescoredAssessment = riskPredictors()

        whenever(clock.instant()).thenReturn(Instant.parse("2025-03-16T09:30:00Z"))
        whenever(clock.zone).thenReturn(ZoneOffset.UTC)
        whenever(deliusApiService.getTierToDelius(crn)).thenReturn(deliusInputs())
        whenever(assessmentApiService.getTierAssessmentInformation(crn)).thenReturn(null)
        whenever(assessmentApiService.getRiskPredictors(crn)).thenReturn(null)
        whenever(rescoredAssessmentService.getByCrn(crn)).thenReturn(rescoredAssessment)
        whenever(tierUpdater.updateTier(any(), eq(crn))).thenReturn(true)

        val result = tierCalculationService.calculateTierForCrn(crn, RecalculationSource.FullRecalculation)

        assertThat(result).isNotNull
        assertThat(result!!.data.tier).isEqualTo(Tier.C)
        assertThat(result.data.riskPredictors).isSameAs(rescoredAssessment)
        verify(rescoredAssessmentService).getByCrn(crn)
        verify(tierUpdater).updateTier(result, crn)
        verify(domainEventPublisher).update(crn, true, result.uuid)
        verify(telemetryService).trackTierCalculated(result, true, RecalculationSource.FullRecalculation)
    }

    @Test
    fun `flag disabled results in no v3 calculation`() {
        whenever(featureFlags.v3Enabled).thenReturn(false)

        val crn = TestData.crn()

        whenever(clock.instant()).thenReturn(Instant.parse("2025-03-16T09:30:00Z"))
        whenever(clock.zone).thenReturn(ZoneOffset.UTC)
        whenever(deliusApiService.getTierToDelius(crn)).thenReturn(deliusInputs())
        whenever(assessmentApiService.getTierAssessmentInformation(crn)).thenReturn(null)
        whenever(assessmentApiService.getRiskPredictors(crn)).thenReturn(riskPredictors())
        whenever(tierUpdater.updateTier(any(), eq(crn))).thenReturn(true)

        val result = tierCalculationService.calculateTierForCrn(crn, RecalculationSource.FullRecalculation)

        assertThat(result).isNotNull
        assertThat(result!!.data.tier).isNull()
        assertThat(result.data.protect).isNotNull()
        assertThat(result.data.change).isNotNull()
    }

    private fun deliusInputs() = DeliusInputs(
        isFemale = false,
        rsrScore = BigDecimal.ZERO,
        ogrsScore = 0,
        hasNoMandate = false,
        registrations = Registrations(
            hasIomNominal = false,
            hasLiferIpp = false,
            hasDomesticAbuse = false,
            hasStalking = false,
            hasChildProtection = false,
            complexityFactors = emptyList(),
            rosh = null,
            mappaLevel = null,
            mappaCategory = null,
            unsupervised = null,
        ),
        previousEnforcementActivity = false,
        latestReleaseDate = null,
        hasActiveEvent = true,
    )

    private fun riskPredictors() = OGRS4Predictors(
        completedDate = LocalDate.of(2025, 3, 1).atStartOfDay(),
        output = AllPredictorDto(
            allReoffendingPredictor = StaticOrDynamicPredictorDto(
                staticOrDynamic = ScoreType.DYNAMIC,
                score = BigDecimal("75.0"),
            ),
            combinedSeriousReoffendingPredictor = VersionedStaticOrDynamicPredictorDto(
                staticOrDynamic = ScoreType.STATIC,
                score = BigDecimal("1.0"),
            ),
        ),
    )
}
