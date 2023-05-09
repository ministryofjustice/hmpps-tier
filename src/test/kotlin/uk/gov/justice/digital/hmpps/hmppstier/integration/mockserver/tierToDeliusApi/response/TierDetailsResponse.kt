package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response

import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Registration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Requirement
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.TierDetails
import java.time.format.DateTimeFormatter

fun tierDetailsResponse(tierDetails: TierDetails) = """
  {
    "gender": "${tierDetails.gender}",
    ${tierDetails.currentTier?.let { """ "currentTier": "$it", """.trimIndent() } ?: ""}
    
    "registrations": [
      ${tierDetails.registrations.joinToString(",") { getRegistration(it) } }
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

fun getRegistration(registration: Registration) = """
  {
        "code": "${registration.typeCode}",
        "description": "description",
        ${registration.registerLevel?.let { """ "level":"$it", """.trimIndent() } ?: ""}
        "date": "${registration.startDate.format(DateTimeFormatter.ISO_DATE)}"
      }
""".trimIndent()
