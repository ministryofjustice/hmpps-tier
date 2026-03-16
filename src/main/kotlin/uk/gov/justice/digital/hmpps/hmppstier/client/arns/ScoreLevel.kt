package uk.gov.justice.digital.hmpps.hmppstier.client.arns

enum class ScoreLevel(val type: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    VERY_HIGH("Very High"),
    NOT_APPLICABLE("Not Applicable"),
}
