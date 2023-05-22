package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response

fun offenderResponse(gender: String?, currentTier: String?) = """
  {
    ${gender?.let { """ "gender": "$gender",""".trimIndent()} ?: ""}
    ${currentTier?.let { """ "ogrsScore": "$currentTier" """.trimIndent() } ?: ""}
  }
""".trimIndent()
