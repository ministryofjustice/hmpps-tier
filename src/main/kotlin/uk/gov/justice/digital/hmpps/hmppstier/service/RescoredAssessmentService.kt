package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.OGRS4Predictors
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ScoreLevel
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ScoreType
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.AllPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.BasePredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.StaticOrDynamicPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.client.arns.ogrs4.VersionedStaticOrDynamicPredictorDto
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.RescoredAssessmentRepository

@Service
class RescoredAssessmentService(
    private val rescoredAssessmentRepository: RescoredAssessmentRepository,
) {
    fun getByCrn(crn: String) =
        rescoredAssessmentRepository.findFirstByCrnOrderByCompletedDateDesc(crn)?.let {
            OGRS4Predictors(
                completedDate = it.completedDate.atStartOfDay(),
                outputVersion = "2",
                output = AllPredictorDto(
                    allReoffendingPredictor = StaticOrDynamicPredictorDto(
                        staticOrDynamic = it.arpIsDynamic.toScoreType(),
                        score = it.arpScore.toBigDecimal(),
                        band = it.arpBand.toScoreLevel(),
                    ),
                    directContactSexualReoffendingPredictor = BasePredictorDto(
                        score = it.dcSrpScore.toBigDecimal(),
                        band = it.dcSrpBand.toScoreLevel(),
                    ),
                    indirectImageContactSexualReoffendingPredictor = BasePredictorDto(
                        score = it.iicSrpScore.toBigDecimal(),
                        band = it.iicSrpBand.toScoreLevel(),
                    ),
                    combinedSeriousReoffendingPredictor = VersionedStaticOrDynamicPredictorDto(
                        staticOrDynamic = it.csrpIsDynamic.toScoreType(),
                        score = it.csrpScore.toBigDecimal(),
                        band = it.csrpBand.toScoreLevel(),
                    )
                ),
            )
        }

    private fun Boolean.toScoreType() = if (this) ScoreType.DYNAMIC else ScoreType.STATIC
    private fun String.toScoreLevel() =
        if (this === "NA") ScoreLevel.NOT_APPLICABLE else ScoreLevel.entries.find { it.type == this }
}
