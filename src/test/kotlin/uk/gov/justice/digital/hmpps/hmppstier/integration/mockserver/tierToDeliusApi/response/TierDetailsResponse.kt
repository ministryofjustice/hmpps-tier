package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response

import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Requirement
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.TierDetails
import java.time.format.DateTimeFormatter

fun tierDetailsResponse(tierDetails: TierDetails) = """
  {
    "gender": "${tierDetails.gender}",
    ${tierDetails.currentTier?.let { """ "currentTier": "$it", """.trimIndent() } ?: ""}
    
    "registrations": [
      {
        "code": "ALT2",
        "description": "ALT Public Interest",
        "date": "2023-04-17"
      }
    ],
    "convictions": [
      ${tierDetails.convictions.joinToString(",") { getConviction(it) } }
    ]
    ${tierDetails.ogrsScore?.let { """ ,"ogrsscore": "$it" """.trimIndent() } ?: "" }
    ${tierDetails.rsrScore?.let { """ ,"rsrscore": "$it" """.trimIndent() } ?: "" }
  }
""".trimIndent()

fun getConviction(conviction: Conviction) = """
  {
      ${conviction.terminationDate?.let { """ "terminationDate": "${it.format(DateTimeFormatter.ISO_DATE)}", """.trimIndent() } ?: ""}
      "sentenceTypeCode": "${conviction.sentenceCode}",
      "sentenceTypeDescription": "Description",
      "breached": ${conviction.breached},
      "requirements": [
        ${conviction.requirements.joinToString(",") { getRequirement(it) } }
      ]
  }
""".trimIndent()

fun getRequirement(requirement: Requirement) = """
    {
      "mainCategoryTypeCode": "${requirement.mainTypeCode}",
      "restrictive": ${requirement.restrictive}
    }
""".trimIndent()
