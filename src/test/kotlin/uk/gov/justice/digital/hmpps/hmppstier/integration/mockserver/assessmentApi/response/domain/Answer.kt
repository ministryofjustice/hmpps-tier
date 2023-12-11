package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.response.domain

data class Answer(
    val questionCode: String,
    val questionText: String,
    val answerCode: String,
)
