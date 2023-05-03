package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response

import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.domain.Sentence
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun convictionsResponse(vararg convictions: Conviction) = """
  [
    ${convictions.joinToString(",") { getConviction(it) }}
  ]
""".trimIndent()

private fun getConviction(conviction: Conviction) = """
  {
      "convictionId": ${conviction.id},
      "index": "1",
      "active": ${conviction.active},
      "inBreach": false,
      "convictionDate": "${conviction.convictionDate.format(DateTimeFormatter.ISO_DATE)}",
      "referralDate": "2018-07-13",
      "offences": [
        {
          "offenceId": "M2500222290",
          "mainOffence": true,
          "detail": {
            "code": "01600",
            "description": "(Buggery and attempted buggery - 01600)",
            "mainCategoryCode": "016",
            "mainCategoryDescription": "Buggery and attempted buggery",
            "mainCategoryAbbreviation": "Buggery and attempted buggery",
            "ogrsOffenceCategory": "Sexual (not against child)",
            "subCategoryCode": "00",
            "subCategoryDescription": "Buggery and attempted buggery",
            "form20Code": "31"
          },
          "offenceDate": "2018-07-13T00:00:00",
          "offenceCount": 1,
          "offenderId": 2500252389,
          "createdDatetime": "2018-07-13T13:20:47",
          "lastUpdatedDatetime": "2018-07-13T13:20:47"
        }
      ],
      ${getSentence(conviction.sentence)}
      ${getCustody(conviction.sentence)}
      "latestCourtAppearanceOutcome": {
        "code": "123",
        "description": "Appearance"
      }
  }
""".trimIndent()

private fun getSentence(sentence: Sentence?) = sentence?.let {
  """
      "sentence": {
        "sentenceId": 2500212176,
        "description": "Description",
        "originalLength": 1,
        "originalLengthUnits": "Months",
        "defaultLength": 1,
        "lengthInDays": 30,
        "expectedSentenceEndDate": "${LocalDate.of(2021, 4, 30).plusMonths(sentence.sentenceLength).format(DateTimeFormatter.ISO_DATE)}",
        "startDate": "${LocalDate.of(2021, 4, 30).format(DateTimeFormatter.ISO_DATE)}",
        ${it.terminationDate?.let { terminationDate ->
    """
            "terminationDate":"${terminationDate.format(DateTimeFormatter.ISO_DATE)}",
            "terminationReason": "Auto Terminated",
    """.trimIndent()
  } ?: ""}
        "sentenceType": {
          "code": "${sentence.sentenceCode}",
          "description": "Description"
        }
      },
  """.trimIndent()
} ?: ""

private fun getCustody(sentence: Sentence?) = sentence?.takeIf { setOf("NC", "SC").contains(it.sentenceCode) }?.let {
  """
    "custody": {
        "institution": {
          "institutionId": 157,
          "isEstablishment": true,
          "code": "UNKNOW",
          "description": "Unknown",
          "institutionName": "Unknown",
          "establishmentType": {
            "code": "E",
            "description": "Prison"
          }
        },
        "keyDates": {},
        "status": {
          "code": "A",
          "description": "Sentenced - In Custody"
        },
        "sentenceStartDate": "2018-07-13"
      },
  """.trimIndent()
} ?: ""
