package uk.gov.justice.digital.hmpps.hmppstier.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.client.SectionAnswer
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES

class AdditionalFactorsForWomenTest {

    @Test
    fun `should add multiple additional factors`() {
        val result = AdditionalFactorsForWomen.calculate(
            mapOf(
                PARENTING_RESPONSIBILITIES to SectionAnswer.YesNo.Yes,
                AdditionalFactorForWomen.TEMPER_CONTROL to SectionAnswer.Problem.Some,
            ),
            offenderIsFemale = true,
            previousEnforcementActivity = false,
        )
        assertThat(result).isEqualTo(4)
    }

    @Test
    fun `should not include additional factors if no valid assessment`() {
        val result = AdditionalFactorsForWomen.calculate(
            null,
            offenderIsFemale = true,
            previousEnforcementActivity = false,
        )
        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `should count both Temper and Impulsivity as max '1'`() {
        val result = AdditionalFactorsForWomen.calculate(
            mapOf(
                AdditionalFactorForWomen.IMPULSIVITY to SectionAnswer.Problem.Significant,
                AdditionalFactorForWomen.TEMPER_CONTROL to SectionAnswer.Problem.Some,
            ),
            offenderIsFemale = true,
            previousEnforcementActivity = false,
        )
        assertThat(result).isEqualTo(2) // 1 * 2 weighting for all additional factors
    }

    @Test
    fun `should count Temper without Impulsivity as max '2'`() {
        val result = AdditionalFactorsForWomen.calculate(
            mapOf(
                AdditionalFactorForWomen.TEMPER_CONTROL to SectionAnswer.Problem.Some,
            ),
            offenderIsFemale = true,
            previousEnforcementActivity = false,
        )
        assertThat(result).isEqualTo(2) // 1 * 2 weighting for all additional factors
    }

    @Test
    fun `should count Impulsivity without Temper as max '1'`() {
        val result = AdditionalFactorsForWomen.calculate(
            mapOf(
                AdditionalFactorForWomen.IMPULSIVITY to SectionAnswer.Problem.Significant,
            ),
            offenderIsFemale = true,
            previousEnforcementActivity = false,
        )
        assertThat(result).isEqualTo(2) // 1 * 2 weighting for all additional factors
    }

    @Test
    fun `should ignore negative Parenting`() {
        val result = AdditionalFactorsForWomen.calculate(
            mapOf(
                PARENTING_RESPONSIBILITIES to SectionAnswer.YesNo.No,
            ),
            offenderIsFemale = true,
            previousEnforcementActivity = false,
        )
        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `should ignore negative Impulsivity`() {
        val result = AdditionalFactorsForWomen.calculate(
            mapOf(
                AdditionalFactorForWomen.IMPULSIVITY to SectionAnswer.Problem.None,
            ),
            offenderIsFemale = true,
            previousEnforcementActivity = false,
        )
        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `should ignore negative Temper`() {
        val result = AdditionalFactorsForWomen.calculate(
            mapOf(
                AdditionalFactorForWomen.TEMPER_CONTROL to SectionAnswer.Problem.None,
            ),
            offenderIsFemale = true,
            previousEnforcementActivity = false,
        )
        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `Should return Breach points if previousEnforcementActivity is true`() {
        val result = AdditionalFactorsForWomen.calculate(
            null,
            offenderIsFemale = true,
            previousEnforcementActivity = true,
        )
        assertThat(result).isEqualTo(2)
    }

    @Test
    fun `Should return no Breach points if previousEnforcementActivity is false`() {
        val result = AdditionalFactorsForWomen.calculate(
            null,
            offenderIsFemale = true,
            previousEnforcementActivity = false,
        )
        assertThat(result).isEqualTo(0)
    }
}
