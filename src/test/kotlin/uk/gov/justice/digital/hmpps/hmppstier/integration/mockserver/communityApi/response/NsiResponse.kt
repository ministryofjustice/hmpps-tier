package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response

fun nsiResponse(code: String?) = """
{
  "nsis":[
    ${code?.let { getNsiOutcome(code) } ?: ""}
  ]
}
""".trimIndent()

fun getNsiOutcome(code: String) = """
    {
      "nsiOutcome": {
        "code": "$code"
      }
    }
""".trimIndent()
