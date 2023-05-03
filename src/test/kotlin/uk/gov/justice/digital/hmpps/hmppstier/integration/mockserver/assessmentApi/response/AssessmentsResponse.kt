package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.response

import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.response.domain.Assessment
import java.time.format.DateTimeFormatter

fun assessmentsResponse(vararg assessments: Assessment) = """
  [
     ${assessments.joinToString(",") { getAssessment(it) } }
  ]
""".trimIndent()


fun getAssessment(assessment: Assessment) = """
  {
    "assessmentId": ${assessment.assessmentId},
    "refAssessmentVersionCode": "LAYER3",
    "refAssessmentVersionNumber": "1",
    "refAssessmentId": 1,
    "assessmentType": "LAYER_3",
    "assessmentStatus": "${assessment.status}",
    "historicStatus": "CURRENT",
    "refAssessmentOasysScoringAlgorithmVersion": 1,
    "assessorName": "Layer 3",
    "created": "2023-05-03T09:26:28.328Z",
    "completed": " ${assessment.completedDate.format(DateTimeFormatter.ISO_DATE_TIME)}",
    "voided": ""
  }
""".trimIndent()