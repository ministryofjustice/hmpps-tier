package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.response

import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.response.domain.Need

fun needsResponse(vararg needs: Need) = """
  [
    ${needs.joinToString(",") { getNeed(it) }}
  ]
""".trimIndent()

fun getNeed(need: Need) = """
  {
    "section": "${need.section}",
    "name": "${need.name}",
    "overThreshold": false,
    "riskOfHarm": true,
    "riskOfReoffending": true,
    "flaggedAsNeed": true,
    "severity": "${need.severity}",
    "identifiedAsNeed": true,
    "needScore": 2
  }
""".trimIndent()
