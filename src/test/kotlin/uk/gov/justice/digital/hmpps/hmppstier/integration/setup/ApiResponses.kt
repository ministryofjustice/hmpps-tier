package uk.gov.justice.digital.hmpps.hmppstier.integration.setup

import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType.APPLICATION_JSON
import java.nio.file.Files.readString
import java.nio.file.Paths

const val communityApiPath: String = "src/test/resources/fixtures/community-api"
const val assessmentApiPath: String = "src/test/resources/fixtures/assessment-api"

fun communityApiAssessmentsResponse(): HttpResponse =
  jsonResponseFromPath("$communityApiPath/assessments.json")

fun emptyCommunityApiAssessmentsResponse(): HttpResponse = jsonResponseOf("{}")

fun emptyNsisResponse(): HttpResponse = jsonResponseOf("{\"nsis\": []}")

fun registrationsResponse(): HttpResponse =
  jsonResponseFromPath("$communityApiPath/registrations.json")

fun emptyRegistrationsResponse(): HttpResponse = jsonResponseOf("{}")

fun registrationsResponseWithNoLevel(): HttpResponse =
  jsonResponseFromPath("$communityApiPath/registrations-no-level.json")

fun custodialSCConvictionResponse(): HttpResponse =
  jsonResponseFromPath("$communityApiPath/convictions-custodial-sc.json")

fun custodialNCConvictionResponse(): HttpResponse =
  jsonResponseFromPath("$communityApiPath/convictions-custodial-nc.json")

fun custodialTerminatedConvictionResponse(): HttpResponse =
  jsonResponseFromPath("$communityApiPath/convictions-custodial-terminated.json")

fun nonCustodialConvictionResponse(): HttpResponse =
  jsonResponseFromPath("$communityApiPath/convictions-non-custodial.json")

fun noSentenceConvictionResponse(): HttpResponse =
  jsonResponseFromPath("$communityApiPath/convictions-no-sentence.json")

fun nonCustodialTerminatedConvictionResponse(): HttpResponse =
  jsonResponseFromPath("$communityApiPath/convictions-non-custodial-terminated.json")

fun maleOffenderResponse(): HttpResponse =
  jsonResponseFromPath("$communityApiPath/offender-male.json")

fun femaleOffenderResponse(): HttpResponse =
  jsonResponseFromPath("$communityApiPath/offender-female.json")

fun restrictiveRequirementsResponse(): HttpResponse =
  jsonResponseFromPath("$communityApiPath/requirements-restrictive.json")

fun restrictiveAndNonRestrictiveRequirementsResponse(): HttpResponse =
  jsonResponseFromPath("$communityApiPath/requirements-restrictive-and-non-restrictive.json")

fun nonRestrictiveRequirementsResponse(): HttpResponse =
  jsonResponseFromPath("$communityApiPath/requirements-non-restrictive.json")

fun unpaidWorkRequirementsResponse(): HttpResponse =
  jsonResponseFromPath("$communityApiPath/requirements-unpaid-work.json")

fun unpaidWorkWithOrderLengthExtendedAndAdditionalHoursRequirementsResponse(): HttpResponse = jsonResponseFromPath("$communityApiPath/requirements-unpaid-work-additional-hours-order-length-extended.json")

fun noRequirementsResponse(): HttpResponse = jsonResponseOf(
  "{\n" +
    "    \"requirements\": []\n" +
    "}"
)

fun additionalRequirementsResponse(): HttpResponse = jsonResponseFromPath("$communityApiPath/requirements-additional.json")

fun custodialAndNonCustodialConvictions(): HttpResponse =
  jsonResponseFromPath("$communityApiPath/convictions-custodial-and-non-custodial.json")

fun nonCustodialCurrentAndTerminatedConviction(): HttpResponse =
  jsonResponseFromPath("$communityApiPath/convictions-non-custodial-current-and-terminated.json")

fun assessmentsApiAssessmentsResponse(year: Int): HttpResponse = jsonResponseOf(
  responseFrom("$assessmentApiPath/assessments.json")
    .replace("completedDate", "$year-01-01T00:00:00")
    .replace("voidedDate", "")
)

fun assessmentsApiNoSeverityNeedsResponse(): HttpResponse =
  jsonResponseFromPath("$assessmentApiPath/no_severity_needs.json")

fun assessmentsApi8NeedsResponse(): HttpResponse =
  jsonResponseFromPath("$assessmentApiPath/8_points_needs.json")

fun assessmentsApiHighSeverityNeedsResponse(): HttpResponse =
  jsonResponseFromPath("$assessmentApiPath/high_severity_needs_18_points.json")

private fun responseFrom(path: String) =
  readString(Paths.get(path))

private fun jsonResponseOf(response: String): HttpResponse =
  HttpResponse.response().withContentType(APPLICATION_JSON).withBody(response)

private fun jsonResponseFromPath(path: String): HttpResponse =
  jsonResponseOf(responseFrom(path))
