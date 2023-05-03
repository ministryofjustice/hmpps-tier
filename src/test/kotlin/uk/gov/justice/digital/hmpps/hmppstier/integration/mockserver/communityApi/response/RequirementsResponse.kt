package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response

import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.domain.Requirement

fun requirementsResponse(vararg requirements: Requirement) = """
  {
    "requirements": [
      ${requirements.joinToString(",") { getRequirement(it) }}
    ]
  }
""".trimIndent()

fun getRequirement(requirement: Requirement) = """
  {
        "requirementId": 2500159141,
        "startDate": "2021-01-19",
        "active": true
        ${getTypeCategory("requirementTypeSubCategory", requirement.subTypeCode)}
        ${getTypeCategory("requirementTypeMainCategory", requirement.mainTypeCode)}
        ${getTypeCategory("adRequirementTypeMainCategory", requirement.additionalMainTypeCode)}
        ${getTypeCategory("adRequirementTypeSubCategory", requirement.additionalSubTypeCode)}
        ${requirement.length?.let { """ ,"length": $it """.trimIndent() } ?: ""}
        ${requirement.lengthUnit?.let {""" ,"lengthUnit": "$it" """.trimIndent()} ?: ""}
        ${requirement.restrictive?.let { """ ,"restrictive": $it """ } ?: ""}
      }
""".trimIndent()

fun getTypeCategory(category: String, code: String?) = code?.let {
  """
    ,"$category": {
          "code": "$code",
          "description": "Description"
        }
  """.trimIndent()
} ?: ""
