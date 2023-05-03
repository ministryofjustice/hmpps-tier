package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response

import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.domain.Registration
import java.time.format.DateTimeFormatter

fun registrationResponse(vararg registrations: Registration) = """
  {
    "registrations": [
      ${registrations.joinToString(",") { getRegistration(it) }}
    ]
  }
""".trimIndent()

fun getRegistration(registration: Registration) = """
  {
    "registrationId": 2500155758,
    "offenderId": 2500342345,
    "register": {
      "code": "5",
      "description": "Public Protection"
    },
    "type": {
      "code": "${registration.typeCode}",
      "description": "MAPPA"
    },
    "riskColour": "Red",
    "startDate": "${registration.startDate.format(DateTimeFormatter.ISO_DATE)}",
    "nextReviewDate": "2021-05-01",
    "reviewPeriodMonths": 3,
    "notes": "X320741 registering MAPPA cat 2 level 2",
    "registeringTeam": {
      "code": "N07CHT",
      "description": "Automation SPG"
    },
    "registeringOfficer": {
      "code": "N07A060",
      "forenames": "NDelius26",
      "surname": "NDelius26",
      "unallocated": false
    },
    "registeringProbationArea": {
      "code": "N07",
      "description": "NPS London"
    },
    ${registration.registerLevel?.let {
  """
            "registerLevel": {
              "code": "$it",
              "description": "MAPPA Level 2"
            },
  """.trimIndent()
} ?: ""}
    "registerCategory": {
      "code": "M2",
      "description": "MAPPA Cat 2"
    },
    "warnUser": false,
    "active": true,
    "numberOfPreviousDeregistrations": 0
  }
""".trimIndent()
