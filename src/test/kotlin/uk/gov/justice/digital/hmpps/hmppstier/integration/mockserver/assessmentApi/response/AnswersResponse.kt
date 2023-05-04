package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.response

import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.response.domain.Answer

fun answersResponse(assessmentId: Long, vararg answers: Answer) = """
  {
    "assessmentId": "$assessmentId",
    "questionAnswers": [
        ${answers.joinToString(",") { getAnswer(it) } }
    ]
  }
""".trimIndent()

fun getAnswer(answer: Answer) = """
  {
    "refQuestionId": 1768,
    "refQuestionCode": "${answer.questionCode}",
    "oasysQuestionId": 38336258988,
    "displayOrder": 600,
    "questionText": "${answer.questionText}",
    "currentlyHidden": false,
    "disclosed": false,
    "answers": [
      {
        "refAnswerCode": "${answer.answerCode}",
        "oasysAnswerId": 31093923299,
        "refAnswerId": 2012,
        "displayOrder": 15,
        "staticText": "2-Significant problems",
        "ovpScore": 2
      }
    ]
  }
""".trimIndent()
