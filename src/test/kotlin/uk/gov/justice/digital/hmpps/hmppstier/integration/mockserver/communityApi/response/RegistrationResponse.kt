package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response

fun registrationResponse() = """
{
  "registrations":[
    {
      "type": {
        "code": "MAPP"
      },
      "registerLevel": {
        "code": "M1"
      },
      "startDate": "2021-02-01"
    }
  ]
}
""".trimIndent()
