package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response

fun convictionResponse(convictionId: String?) = """
[
    ${convictionId?.let { getConviction(convictionId) } ?: ""}
]
""".trimIndent()

fun getConviction(convictionId: String?) = """
  {
    ${convictionId?.let { getConvictionId() } ?: ""}
    ${convictionId?.let { getSentence() } ?: ""}
  }
""".trimIndent()

fun getSentence() = """
    "sentence": {
      "terminationDate": "2021-02-01",
      "sentenceType": {
        "code": "NC"
      }
    }
""".trimIndent()

fun getConvictionId() = """
    "convictionId": 12345,
""".trimIndent()
