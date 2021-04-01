package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType.APPLICATION_JSON
import java.nio.file.Files.readString
import java.nio.file.Paths

fun communityApiAssessmentsResponse(): HttpResponse =
  jsonResponseOf(responseFrom("src/test/resources/fixtures/community-api/assessments.json"))

fun emptyCommunityApiAssessmentsResponse(): HttpResponse = jsonResponseOf("{}")

fun emptyNsisResponse(): HttpResponse = jsonResponseOf("{\"nsis\": []}")

fun registrationsResponse(): HttpResponse =
  jsonResponseOf(responseFrom("src/test/resources/fixtures/community-api/registrations.json"))

fun emptyRegistrationsResponse(): HttpResponse = jsonResponseOf("{}")

fun registrationsResponseWithNoLevel(): HttpResponse = jsonResponseOf(
  responseFrom("src/test/resources/fixtures/community-api/registrations-no-level.json")
)

fun custodialSCConvictionResponse(): HttpResponse = jsonResponseOf(
  responseFrom("src/test/resources/fixtures/community-api/convictions-custodial-sc.json")
)

fun custodialNCConvictionResponse(): HttpResponse = jsonResponseOf(
  responseFrom("src/test/resources/fixtures/community-api/convictions-custodial-nc.json")
)

fun custodialTerminatedConvictionResponse(): HttpResponse = jsonResponseOf(
  responseFrom("src/test/resources/fixtures/community-api/convictions-custodial-terminated.json")
)

fun nonCustodialConvictionResponse(): HttpResponse = jsonResponseOf(
  responseFrom("src/test/resources/fixtures/community-api/convictions-non-custodial.json")
)

fun noSentenceConvictionResponse(): HttpResponse = jsonResponseOf(
  responseFrom("src/test/resources/fixtures/community-api/convictions-no-sentence.json")
)

fun nonCustodialTerminatedConvictionResponse(): HttpResponse = jsonResponseOf(
  responseFrom("src/test/resources/fixtures/community-api/convictions-non-custodial-terminated.json")
)

fun maleOffenderResponse(): HttpResponse = jsonResponseOf(
  responseFrom("src/test/resources/fixtures/community-api/offender-male.json")
)

fun femaleOffenderResponse(): HttpResponse = jsonResponseOf(
  responseFrom("src/test/resources/fixtures/community-api/offender-female.json")
)

fun restrictiveRequirementsResponse(): HttpResponse = jsonResponseOf(
  responseFrom("src/test/resources/fixtures/community-api/requirements-restrictive.json")
)

fun restrictiveAndNonRestrictiveRequirementsResponse(): HttpResponse = jsonResponseOf(
  responseFrom("src/test/resources/fixtures/community-api/requirements-restrictive-and-non-restrictive.json")
)

fun nonRestrictiveRequirementsResponse(): HttpResponse = jsonResponseOf(
  responseFrom("src/test/resources/fixtures/community-api/requirements-non-restrictive.json")
)

fun unpaidWorkRequirementsResponse(): HttpResponse = jsonResponseOf(
  responseFrom("src/test/resources/fixtures/community-api/requirements-unpaid-work.json")
)

fun noRequirementsResponse(): HttpResponse = jsonResponseOf(
  "{\n" +
    "    \"requirements\": []\n" +
    "}"
)

fun custodialAndNonCustodialConvictions(): HttpResponse = jsonResponseOf(
  responseFrom("src/test/resources/fixtures/community-api/convictions-custodial-and-non-custodial.json")
)

fun nonCustodialCurrentAndTerminatedConviction(): HttpResponse = jsonResponseOf(
  responseFrom("src/test/resources/fixtures/community-api/convictions-non-custodial-current-and-terminated.json")
)

fun assessmentsApiAssessmentsResponse(year: Int): String =
  responseFrom("src/test/resources/fixtures/assessment-api/assessments.json")
    .replace("completedDate", "$year-01-01T00:00:00")
    .replace("voidedDate", "")

fun assessmentsApiNoSeverityNeedsResponse(): HttpResponse = jsonResponseOf(
  responseFrom("src/test/resources/fixtures/assessment-api/no_severity_needs.json")
)

fun assessmentsApi8NeedsResponse(): HttpResponse = jsonResponseOf(
  responseFrom("src/test/resources/fixtures/assessment-api/8_points_needs.json")
)

fun assessmentsApiHighSeverityNeedsResponse(): HttpResponse = jsonResponseOf(
  responseFrom("src/test/resources/fixtures/assessment-api/high_severity_needs_18_points.json")
)

private fun responseFrom(path: String) =
  readString(Paths.get(path))

private fun jsonResponseOf(response: String): HttpResponse =
  HttpResponse.response().withContentType(APPLICATION_JSON).withBody(response)
