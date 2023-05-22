package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response

fun requirementResponse() = """
{
  "requirements": [
    {
      "restrictive": "false",
      "requirementTypeMainCategory": {
        "code": "X"
      }
    }
  ]
}
""".trimIndent()
