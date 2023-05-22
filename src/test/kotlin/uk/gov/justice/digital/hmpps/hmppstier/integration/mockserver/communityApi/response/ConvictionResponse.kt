package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response

fun convictionResponse() = """
[
  {
    "convictionId": 12345,
    "sentence": {
      "terminationDate": "2021-02-01",
      "sentenceType": {
        "code": "NC"
      }
    }
  }
]
""".trimIndent()
