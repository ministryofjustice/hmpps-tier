package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response

import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.TierDetails

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
      {
        "sentenceTypeCode": "329",
        "sentenceTypeDescription": "ORA Community Order",
        "breached": false,
        "requirements": [
          {
            "mainCategoryTypeCode": "M",
            "restrictive": true
          }
        ]
      }
    ],
    ${tierDetails.ogrsScore?.let { """ "ogrsscore": "$it", """.trimIndent()  } ?: "" }
    ${tierDetails.rsrScore?.let { """ "rsrscore": "$it" """.trimIndent() } ?: "" }
    
  }
""".trimIndent()