package uk.gov.justice.digital.hmpps.hmppstier.client.arns

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.NeedSeverity

class NeedSectionTest {

    @Test
    fun `score and severity are absent when all answers are missing or unknown`() {
        val section = NeedSection.Accommodation(
            questionAnswers = hashMapOf(
                "noFixedAbodeOrTransient" to SectionAnswer.YesNo.Unknown,
                "suitabilityOfAccommodation" to SectionAnswer.Problem.Missing,
                "permanenceOfAccommodation" to SectionAnswer.Problem.Missing,
            ),
        )

        assertThat(section.getScore()).isNull()
        assertThat(section.getSeverity(sanIndicator = false)).isNull()
    }

    @Test
    fun `SAN indicator uses section override thresholds when available`() {
        val section = NeedSection.Accommodation(
            questionAnswers = hashMapOf(
                "suitabilityOfAccommodation" to SectionAnswer.Problem.Significant,
                "permanenceOfAccommodation" to SectionAnswer.Problem.Significant,
                "locationOfAccommodation" to SectionAnswer.Problem.Some,
            ),
        )

        assertThat(section.getScore()).isEqualTo(5)
        assertThat(section.getSeverity(sanIndicator = false)).isEqualTo(NeedSeverity.STANDARD)
        assertThat(section.getSeverity(sanIndicator = true)).isEqualTo(NeedSeverity.SEVERE)
    }

    @Test
    fun `SAN indicator falls back to normal thresholds when no override exists`() {
        val section = NeedSection.EducationTrainingEmployability(
            questionAnswers = hashMapOf(
                "unemployed" to SectionAnswer.Problem.Significant,
                "employmentHistory" to SectionAnswer.Problem.Some,
            ),
        )

        assertThat(section.getScore()).isEqualTo(3)
        assertThat(section.getSeverity(sanIndicator = true)).isEqualTo(NeedSeverity.STANDARD)
    }
}
