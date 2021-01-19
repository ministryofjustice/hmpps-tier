package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.dto.TierDto
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import java.time.Clock
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AssessmentComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.RsrThresholds

@Service
class TierCalculationService(
  private val communityApiDataService: CommunityApiDataService,
  private val assessmentApiDataService: AssessmentApiDataService,
  private val tierCalculationRepository: TierCalculationRepository,
  private val clock: Clock
) {

  fun getTierByCrn(crn: String): TierDto {
    val result = getLatestTierCalculation(crn) ?: calculateTierForCrn(crn)
    log.info("Returned tier for $crn")
    return TierDto.from(result.data)
  }

  fun calculateTierForCrn(crn: String): TierCalculationEntity {
    log.debug("Calculating tier for $crn using 'New' calculation")


    val protectLevel = calculateProtectLevel(crn)
    val changeLevel = calculateChangeLevel(crn)

    val calculation = TierCalculationEntity(
      crn = crn,
      created = LocalDateTime.now(clock),
      data = TierCalculationResultEntity(change = changeLevel, protect = protectLevel)
    )

    log.info("Calculated tier for $crn using 'New' calculation")
    return tierCalculationRepository.save(calculation)
  }

  private fun getLatestTierCalculation(crn: String): TierCalculationEntity? {
    log.debug("Finding latest tier calculation for $crn")

    val calculation = tierCalculationRepository.findFirstByCrnOrderByCreatedDesc(crn)

    if (calculation == null) {
      log.info("No tier calculation found for $crn")
    } else {
      log.info("Found latest tier calculation for $crn")
    }
    return calculation
  }

  fun calculateProtectLevel(crn: String): TierLevel<ProtectLevel> {
    val riskPoints = getRiskPoints(crn)
    val mappaPoints = getMappaPoints(crn)
    val complexityPoints = getComplexityPoints(crn)

    val totalPoints = riskPoints + mappaPoints + complexityPoints
    val tier = when {
      totalPoints >= 30 -> ProtectLevel.A
      totalPoints in 20..29 -> ProtectLevel.B
      totalPoints in 10..19 -> ProtectLevel.C
      else -> ProtectLevel.D
    }

    return TierLevel(tier, totalPoints)
  }

  fun calculateChangeLevel(crn : String): TierLevel<ChangeLevel> {
    val score = getOasysNeedsPoints(crn).plus(getOgrsPoints(crn))
    val tier = when {
      score >= 20 -> ChangeLevel.THREE
      score in 10..19 -> ChangeLevel.TWO
      else -> ChangeLevel.ONE
    }

    return TierLevel(tier, score)
  }

  private fun getRiskPoints(crn: String): Int {
    return maxOf(getRsrPoints(crn),getRoshPoints(crn))
  }

  private fun getRsrPoints(crn: String): Int {
    return communityApiDataService.getRSR(crn).let {
      when {
        it != null && it >= RsrThresholds.TIER_B_RSR.num -> 20
        it != null && it >= RsrThresholds.TIER_C_RSR.num -> 10
        else -> 0
      }
    }
  }

  private fun getRoshPoints(crn: String): Int {
    return communityApiDataService.getRosh(crn).let {
      when (it) {
        Rosh.VERY_HIGH -> 30
        Rosh.HIGH -> 20
        Rosh.MEDIUM -> 10
        else -> 0
      }
    }
  }

  private fun getMappaPoints(crn: String): Int {
    return when (communityApiDataService.getMappa(crn)) {
      Mappa.M2, Mappa.M3 -> 30
      Mappa.M1 -> 5
      else ->  0
    }
  }

  private fun getComplexityPoints(crn: String): Int {
    return communityApiDataService.getComplexityFactors(crn).let { factors ->
      factors.distinct().count().let{ points ->
        when (communityApiDataService.isFemaleOffender(crn)) {
          true -> points.plus(getAssessmentComplexityPoints(crn))
          else ->
            when {
              // we don't count IOM_NOMINAL for men so subtract it
              factors.contains(ComplexityFactor.IOM_NOMINAL) -> points.minus(1)
              else -> points
            }
        }
      }
    }.times(2)
  }

  private fun getAssessmentComplexityPoints(crn: String): Int {
    return assessmentApiDataService.getAssessmentComplexityAnswers(crn).let {
      val parentingAnswer = it[AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES]
      val parenting = when {
        isYes(parentingAnswer) -> 1
        else -> 0
      }
      // We dont take the cumulative score, just '1' if at least one of these two is present
      val impulsivityAnswer = it[AssessmentComplexityFactor.IMPULSIVITY]
      val temperControlAnswer = it[AssessmentComplexityFactor.TEMPER_CONTROL]
      val selfControl = when {
        isAnswered(impulsivityAnswer) || isAnswered(temperControlAnswer) -> 1
        else -> 0
      }
      parenting.plus(selfControl)
    }
  }

  private fun isYes(value: String?): Boolean {
    return value != null && (value.equals("YES", true) || value.equals("Y", true))
  }

  private fun isAnswered(value: String?): Boolean {
    return value != null && value.toInt() > 0
  }

  private fun getOasysNeedsPoints(crn: String): Int {
    return assessmentApiDataService.getAssessmentNeeds(crn).let {
      it.entries.sumBy { ent ->
        ent.key.weighting.times(ent.value?.score ?: 0)
      }
    }
  }

  private fun getOgrsPoints(crn: String): Int {
    return communityApiDataService.getOGRS(crn).let {
      it?.div(10) ?: 0
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(TierCalculationService::class.java)
  }
}
