package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response

import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.domain.NSI

fun nsisResponse(vararg nsis: NSI) = """
  {
    "nsis": [
      ${nsis.joinToString(",") { getNSI(it) }}
    ]
  }
""".trimIndent()

fun getNSI(nsi: NSI) = """
  {
  }
""".trimIndent()