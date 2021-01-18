package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.domain.ChangeScores
import uk.gov.justice.digital.hmpps.hmppstier.domain.ProtectScores
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierCalculation
import uk.gov.justice.digital.hmpps.hmppstier.dto.TierDto
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import java.time.Clock
import java.time.LocalDateTime

@Service
class TierCalculationService(
  private val tierCalculation: TierCalculation,
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

    val isFemale = communityApiDataService.isFemaleOffender(crn)

    val protectScores = ProtectScores(
      crn = crn,
      mappaLevel = communityApiDataService.getMappa(crn),
      rsrScore = communityApiDataService.getRSR(crn),
      roshScore = communityApiDataService.getRosh(crn),
      complexityFactors = communityApiDataService.getComplexityFactors(crn),
      assessmentComplexityFactors = if (isFemale) assessmentApiDataService.getAssessmentComplexityAnswers(crn) else emptyMap()
    )

    val changeScores = ChangeScores(
      crn = crn,
      ogrsScore = communityApiDataService.getOGRS(crn),
      need = assessmentApiDataService.getAssessmentNeeds(crn)
    )

    val calculation = TierCalculationEntity(
      crn = crn,
      created = LocalDateTime.now(clock),
      data = TierCalculationResultEntity.from(tierCalculation.calculateTier(protectScores, changeScores, isFemale))
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

  companion object {
    private val log = LoggerFactory.getLogger(TierCalculationService::class.java)
  }
}
