package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response

fun deliusAssessmentResponse(rsr: String?, ogrs: String?) = """
  {
    ${rsr?.let { """ "rsrScore": "$rsr",""".trimIndent()} ?: ""}
    ${ogrs?.let { """ "ogrsScore": "$ogrs" """.trimIndent() } ?: ""}
  }
""".trimIndent()
