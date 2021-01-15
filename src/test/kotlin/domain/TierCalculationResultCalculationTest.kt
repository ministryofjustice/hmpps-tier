package uk.gov.justice.digital.hmpps.hmppstier.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AssessmentComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeScore
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Need
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectScore
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.RsrThresholds
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.TierMatchCriteria
import java.math.BigDecimal

internal class TierCalculationResultCalculationTest {

  val crn = "CRN"

  @Nested
  @DisplayName("Simple Risk tests")
  inner class SimpleRiskTests {
    @Test
    fun `should use RSR when higher than ROSH`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = RsrThresholds.TIER_B_RSR.num.plus(BigDecimal(1)),
        roshScore = Rosh.MEDIUM,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.RSR_USED_OVER_ROSH)
    }

    @Test
    fun `should use ROSH when higher than RSR`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = RsrThresholds.TIER_B_RSR.num,
        roshScore = Rosh.VERY_HIGH,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.ROSH_USED_OVER_RSR)
    }

    @Test
    fun `should use either when RSR is same as ROSH`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = RsrThresholds.TIER_C_RSR.num,
        roshScore = Rosh.MEDIUM,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.RSR_ROSH_EQUAL)
    }
  }

  @Nested
  @DisplayName("Simple RSR tests")
  inner class SimpleRSRTests {

    @Test
    fun `should return 20 for RSR equal to tier B`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = RsrThresholds.TIER_B_RSR.num,
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.tier).isEqualTo(ProtectScore.B)
      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.RSR_IN_TIER_B)
      assertThat(tier.protectScore.score).isEqualTo(20)
    }

    @Test
    fun `should return 20 for RSR greater than tier B`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = RsrThresholds.TIER_B_RSR.num.plus(BigDecimal(1)),
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.tier).isEqualTo(ProtectScore.B)
      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.RSR_IN_TIER_B)
      assertThat(tier.protectScore.score).isEqualTo(20)
    }

    @Test
    fun `should return 10 for RSR equal to tier C`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = RsrThresholds.TIER_C_RSR.num,
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.tier).isEqualTo(ProtectScore.C)
      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.RSR_IN_TIER_C)
      assertThat(tier.protectScore.score).isEqualTo(10)
    }

    @Test
    fun `should return 10 for RSR greater than tier C`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = RsrThresholds.TIER_C_RSR.num.plus(BigDecimal(1)),
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.tier).isEqualTo(ProtectScore.C)
      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.RSR_IN_TIER_C)
      assertThat(tier.protectScore.score).isEqualTo(10)
    }

    @Test
    fun `should return 0 for RSR less than tier C`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = RsrThresholds.TIER_C_RSR.num.minus(BigDecimal(1)),
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.tier).isEqualTo(ProtectScore.D)
      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.RSR_NO_MATCH)
      assertThat(tier.protectScore.score).isEqualTo(0)
    }

    @Test
    fun `should return 0 for RSR null`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.tier).isEqualTo(ProtectScore.D)
      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.RSR_NO_MATCH)
      assertThat(tier.protectScore.score).isEqualTo(0)
    }
  }

  @Nested
  @DisplayName("Simple ROSH tests")
  inner class SimpleRoshTests {

    @Test
    fun `should return 30 for Very High Rosh`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = Rosh.VERY_HIGH,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.tier).isEqualTo(ProtectScore.A)
      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.ROSH_VERY_HIGH)
      assertThat(tier.protectScore.score).isEqualTo(30)
    }

    @Test
    fun `should return 20 for High Rosh`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = Rosh.HIGH,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.tier).isEqualTo(ProtectScore.B)
      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.ROSH_HIGH)
      assertThat(tier.protectScore.score).isEqualTo(20)
    }

    @Test
    fun `should return 10 for High Rosh`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = Rosh.MEDIUM,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.tier).isEqualTo(ProtectScore.C)
      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.ROSH_MEDIUM)
      assertThat(tier.protectScore.score).isEqualTo(10)
    }

    @Test
    fun `should return 0 for No Rosh`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.tier).isEqualTo(ProtectScore.D)
      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.ROSH_NO_MATCH)
      assertThat(tier.protectScore.score).isEqualTo(0)
    }
  }

  @Nested
  @DisplayName("Simple Mappa tests")
  inner class SimpleMappaTests {

    @Test
    fun `should return 30 for Mappa level 2`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = Mappa.M2,
        rsrScore = null,
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.tier).isEqualTo(ProtectScore.A)
      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.MAPPA_LEVEL_2_OR_3)
      assertThat(tier.protectScore.score).isEqualTo(30)
    }

    @Test
    fun `should return 30 for Mappa level 3`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = Mappa.M3,
        rsrScore = null,
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.tier).isEqualTo(ProtectScore.A)
      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.MAPPA_LEVEL_2_OR_3)
      assertThat(tier.protectScore.score).isEqualTo(30)
    }

    @Test
    fun `should return 5 for Mappa level 1`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = Mappa.M1,
        rsrScore = null,
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.tier).isEqualTo(ProtectScore.D)
      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.MAPPA_LEVEL_1)
      assertThat(tier.protectScore.score).isEqualTo(5)
    }

    @Test
    fun `should return 5 for Mappa none`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.tier).isEqualTo(ProtectScore.D)
      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.MAPPA_NO_MATCH)
      assertThat(tier.protectScore.score).isEqualTo(0)
    }
  }

  @Nested
  @DisplayName("Simple Complexity tests")
  inner class SimpleComplexityTests {

    @Test
    fun `should not count complexity factors none`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.tier).isEqualTo(ProtectScore.D)
      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.NO_COMPLEXITY_FACTORS)
      assertThat(tier.protectScore.score).isEqualTo(0)
    }

    @Test
    fun `should not count complexity factors duplicates`() {

      val calculator = TierCalculation()

      // enough complexity factors to otherwise score a 'C'
      val dupeComplexityFactors: List<ComplexityFactor> = listOf(
        ComplexityFactor.VULNERABILITY_ISSUE,
        ComplexityFactor.VULNERABILITY_ISSUE,
        ComplexityFactor.VULNERABILITY_ISSUE,
        ComplexityFactor.VULNERABILITY_ISSUE,
        ComplexityFactor.VULNERABILITY_ISSUE,
        ComplexityFactor.VULNERABILITY_ISSUE
      )

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = null,
        complexityFactors = dupeComplexityFactors,
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.tier).isEqualTo(ProtectScore.D)
      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.INCLUDED_COMPLEXITY_FACTORS)
      assertThat(tier.protectScore.score).isEqualTo(2)
    }

    @Test
    fun `should not count assessment complexity factors duplicates`() {

      val calculator = TierCalculation()

      val assessmentComplexityFactors: Map<AssessmentComplexityFactor, String> = mapOf(
        AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "Y",
        AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "Y",
        AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "Y",
        AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "Y",
        AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "Y",
        AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "Y",
      )

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = assessmentComplexityFactors
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.tier).isEqualTo(ProtectScore.D)
      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.INCLUDED_COMPLEXITY_FACTORS)
      assertThat(tier.protectScore.score).isEqualTo(2)
    }

    @Test
    fun `should count complexity factors C`() {

      val calculator = TierCalculation()

      // enough complexity factors to score a 'C'
      val complexityFactors: List<ComplexityFactor> = listOf(
        ComplexityFactor.TERRORISM,
        ComplexityFactor.MENTAL_HEALTH,
        ComplexityFactor.PUBLIC_INTEREST,
        ComplexityFactor.CHILD_CONCERNS,
        ComplexityFactor.IOM_NOMINAL,
        ComplexityFactor.VULNERABILITY_ISSUE
      )

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = null,
        complexityFactors = complexityFactors,
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.tier).isEqualTo(ProtectScore.C)
      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.INCLUDED_COMPLEXITY_FACTORS)
      assertThat(tier.protectScore.score).isEqualTo(12)
    }

    @Test
    fun `should count complexity factors B`() {

      val calculator = TierCalculation()

      // enough complexity factors to score a 'B'
      val complexityFactors: List<ComplexityFactor> = listOf(
        ComplexityFactor.TERRORISM,
        ComplexityFactor.MENTAL_HEALTH,
        ComplexityFactor.PUBLIC_INTEREST,
        ComplexityFactor.CHILD_CONCERNS,
        ComplexityFactor.IOM_NOMINAL,
        ComplexityFactor.VULNERABILITY_ISSUE,
        ComplexityFactor.RISK_TO_CHILDREN,
        ComplexityFactor.STREET_GANGS,
        ComplexityFactor.ATTEMPTED_SUICIDE_OR_SELF_HARM,
        ComplexityFactor.CHILD_PROTECTION
      )

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = null,
        complexityFactors = complexityFactors,
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.tier).isEqualTo(ProtectScore.B)
      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.INCLUDED_COMPLEXITY_FACTORS)
      assertThat(tier.protectScore.score).isEqualTo(20)
    }

    @Test
    fun `should combine complexity factors `() {

      val calculator = TierCalculation()

      val complexityFactors: List<ComplexityFactor> = listOf(
        ComplexityFactor.TERRORISM
      )

      val assessmentFactors: Map<AssessmentComplexityFactor, String> = mapOf(
        AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES to "Y",
      )

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = null,
        complexityFactors = complexityFactors,
        assessmentComplexityFactors = assessmentFactors
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.tier).isEqualTo(ProtectScore.D)
      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.INCLUDED_COMPLEXITY_FACTORS)
      assertThat(tier.protectScore.score).isEqualTo(4)
    }

    @Test
    fun `should count both Temper and Impulsivity as max '1'`() {

      val calculator = TierCalculation()

      val assessmentFactors: Map<AssessmentComplexityFactor, String> = mapOf(
        AssessmentComplexityFactor.TEMPER_CONTROL to "1",
        AssessmentComplexityFactor.IMPULSIVITY to "2",
      )

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = assessmentFactors
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.tier).isEqualTo(ProtectScore.D)
      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.INCLUDED_COMPLEXITY_FACTORS)
      // 1 complexity factor * 2 is 2
      assertThat(tier.protectScore.score).isEqualTo(2)
    }

    @Test
    fun `should count Temper without Impulsivity as max '1'`() {

      val calculator = TierCalculation()

      val assessmentFactors: Map<AssessmentComplexityFactor, String> = mapOf(
        AssessmentComplexityFactor.TEMPER_CONTROL to "2",
      )

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = assessmentFactors
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.tier).isEqualTo(ProtectScore.D)
      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.INCLUDED_COMPLEXITY_FACTORS)
      // 1 complexity factor * 2 is 2
      assertThat(tier.protectScore.score).isEqualTo(2)
    }

    @Test
    fun `should count Impulsivity without Temper as max '1'`() {

      val calculator = TierCalculation()

      val assessmentFactors: Map<AssessmentComplexityFactor, String> = mapOf(
        AssessmentComplexityFactor.IMPULSIVITY to "2",
      )

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = assessmentFactors
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.protectScore.tier).isEqualTo(ProtectScore.D)
      assertThat(tier.protectScore.criteria).contains(TierMatchCriteria.INCLUDED_COMPLEXITY_FACTORS)
      // 1 complexity factor * 2 is 2
      assertThat(tier.protectScore.score).isEqualTo(2)
    }
  }

  @Nested
  @DisplayName("Simple Ogrs tests")
  inner class SimpleOgrsTests {

    @Test
    fun `should calculate Ogrs none`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.changeScore.tier).isEqualTo(ChangeScore.ONE)
      assertThat(tier.changeScore.criteria).contains(TierMatchCriteria.NO_ORGS)
      assertThat(tier.changeScore.score).isEqualTo(0)
    }

    @Test
    fun `should calculate Ogrs take 10s 50`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = 50,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.changeScore.tier).isEqualTo(ChangeScore.ONE)
      assertThat(tier.changeScore.criteria).contains(TierMatchCriteria.INCLUDED_ORGS)
      assertThat(tier.changeScore.score).isEqualTo(5)
    }

    @Test
    fun `should calculate Ogrs take 10s 51`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = 51,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.changeScore.tier).isEqualTo(ChangeScore.ONE)
      assertThat(tier.changeScore.criteria).contains(TierMatchCriteria.INCLUDED_ORGS)
      assertThat(tier.changeScore.score).isEqualTo(5)
    }

    @Test
    fun `should calculate Ogrs take 10s 59`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = 59,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.changeScore.tier).isEqualTo(ChangeScore.ONE)
      assertThat(tier.changeScore.criteria).contains(TierMatchCriteria.INCLUDED_ORGS)
      assertThat(tier.changeScore.score).isEqualTo(5)
    }
  }

  @Nested
  @DisplayName("Simple Oasys Needs tests")
  inner class SimpleNeedsTests {

    @Test
    fun `should calculate Oasys Needs empty list`() {

      val calculator = TierCalculation()

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = mapOf()
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.changeScore.tier).isEqualTo(ChangeScore.ONE)
      assertThat(tier.changeScore.criteria).contains(TierMatchCriteria.NO_OASYS_NEEDS)
      assertThat(tier.changeScore.score).isEqualTo(0)
    }

    @Test
    fun `should calculate Oasys Needs 10`() {

      val calculator = TierCalculation()

      val needs: Map<Need, NeedSeverity> = mapOf(
        Need.ACCOMMODATION to NeedSeverity.SEVERE, // 2
        Need.EDUCATION_TRAINING_AND_EMPLOYABILITY to NeedSeverity.SEVERE, // 2
        Need.RELATIONSHIPS to NeedSeverity.SEVERE, // 2
        Need.LIFESTYLE_AND_ASSOCIATES to NeedSeverity.SEVERE, // 2
        Need.DRUG_MISUSE to NeedSeverity.SEVERE, // 2
      )

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = needs
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.changeScore.tier).isEqualTo(ChangeScore.TWO)
      assertThat(tier.changeScore.criteria).contains(TierMatchCriteria.INCLUDED_OASYS_NEEDS)
      assertThat(tier.changeScore.score).isEqualTo(10)
    }

    @Test
    fun `should calculate Oasys Needs 20`() {

      val calculator = TierCalculation()

      val needs: Map<Need, NeedSeverity> = mapOf(
        Need.ACCOMMODATION to NeedSeverity.SEVERE, // 2
        Need.EDUCATION_TRAINING_AND_EMPLOYABILITY to NeedSeverity.SEVERE, // 2
        Need.RELATIONSHIPS to NeedSeverity.SEVERE, // 2
        Need.LIFESTYLE_AND_ASSOCIATES to NeedSeverity.SEVERE, // 2
        Need.DRUG_MISUSE to NeedSeverity.SEVERE, // 2
        Need.ALCOHOL_MISUSE to NeedSeverity.SEVERE, // 2
        Need.THINKING_AND_BEHAVIOUR to NeedSeverity.SEVERE, // 4
        Need.ATTITUDES to NeedSeverity.SEVERE, // 4
      )

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = needs
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.changeScore.tier).isEqualTo(ChangeScore.THREE)
      assertThat(tier.changeScore.criteria).contains(TierMatchCriteria.INCLUDED_OASYS_NEEDS)
      assertThat(tier.changeScore.score).isEqualTo(20)
    }
  }

  @Nested
  @DisplayName("Oasys Needs points tests")
  inner class NeedsPointsTests {

    @Test
    fun `should factor weighting into calculation NO_NEED`() {

      val calculator = TierCalculation()

      val needs: Map<Need, NeedSeverity> = mapOf(
        Need.ACCOMMODATION to NeedSeverity.NO_NEED,
      )

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = needs
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.changeScore.tier).isEqualTo(ChangeScore.ONE)
      assertThat(tier.changeScore.criteria).contains(TierMatchCriteria.INCLUDED_OASYS_NEEDS)
      assertThat(tier.changeScore.score).isEqualTo(0)
    }

    @Test
    fun `should factor weighting into calculation STANDARD_NEED`() {

      val calculator = TierCalculation()

      val needs: Map<Need, NeedSeverity> = mapOf(
        Need.ACCOMMODATION to NeedSeverity.STANDARD,
      )

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = needs
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.changeScore.tier).isEqualTo(ChangeScore.ONE)
      assertThat(tier.changeScore.criteria).contains(TierMatchCriteria.INCLUDED_OASYS_NEEDS)
      assertThat(tier.changeScore.score).isEqualTo(1)
    }

    @Test
    fun `should factor weighting into calculation SEVERE_NEED`() {

      val calculator = TierCalculation()

      val needs: Map<Need, NeedSeverity> = mapOf(
        Need.ACCOMMODATION to NeedSeverity.SEVERE,
      )

      val changeScores = ChangeScores(
        crn = crn,
        ogrsScore = null,
        need = needs
      )

      val protectScores = ProtectScores(
        crn = crn,
        mappaLevel = null,
        rsrScore = null,
        roshScore = null,
        complexityFactors = listOf(),
        assessmentComplexityFactors = mapOf()
      )

      val tier = calculator.calculateTier(protectScores, changeScores)

      assertThat(tier.changeScore.tier).isEqualTo(ChangeScore.ONE)
      assertThat(tier.changeScore.criteria).contains(TierMatchCriteria.INCLUDED_OASYS_NEEDS)
      assertThat(tier.changeScore.score).isEqualTo(2)
    }
  }
}
