package uk.gov.justice.digital.hmpps.hmppstier.integration.setup

import org.mockserver.model.HttpResponse
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType.APPLICATION_JSON
import java.math.BigDecimal
import java.nio.file.Files.readString
import java.nio.file.Paths

const val communityApiPath: String = "src/test/resources/fixtures/community-api"
const val assessmentApiPath: String = "src/test/resources/fixtures/assessment-api"

fun communityApiAssessmentsResponse(rsr: BigDecimal): HttpResponse = jsonResponseOf(
  responseFrom("$communityApiPath/assessments.json")
    .replace("rsrScoreToReplace", rsr.toPlainString())
)

fun emptyCommunityApiAssessmentsResponse(): HttpResponse = jsonResponseOf("{}")

fun emptyNsisResponse(): HttpResponse = jsonResponseOf("{\"nsis\": []}")

fun registrationsResponseWithMappa(mappa: String? = "M2"): HttpResponse = jsonResponseOf(
  responseFrom("$communityApiPath/registrations-mappa.json")
    .replace("mappaToReplace", mappa!!)
)

fun registrationsResponseWithRosh(rosh: String): HttpResponse = jsonResponseOf(
  responseFrom("$communityApiPath/registrations-rosh.json")
    .replace("roshToReplace", rosh)
)

fun emptyRegistrationsResponse(): HttpResponse = jsonResponseOf("{}")

fun registrationsResponseWithNoLevel(): HttpResponse = communityApiResponse("registrations-no-level.json")

fun registrationsResponseWithAdditionalFactors(additionalFactors: String): HttpResponse = jsonResponseOf(
  responseFrom("$communityApiPath/registrations-additional.json")
    .replace("additionalFactorsToReplace", additionalFactors)
)

fun custodialSCConvictionResponse(): HttpResponse = communityApiResponse("convictions-custodial-sc.json")

fun custodialNCConvictionResponse(): HttpResponse = communityApiResponse("convictions-custodial-nc.json")

fun custodialTerminatedConvictionResponse(): HttpResponse = communityApiResponse("convictions-custodial-terminated.json")

fun nonCustodialConvictionResponse(): HttpResponse = communityApiResponse("convictions-non-custodial.json")

fun noSentenceConvictionResponse(): HttpResponse = communityApiResponse("convictions-no-sentence.json")

fun nonCustodialTerminatedConvictionResponse(): HttpResponse = communityApiResponse("convictions-non-custodial-terminated.json")

fun maleOffenderResponse(): HttpResponse = communityApiResponse("offender-male.json")

fun femaleOffenderResponse(): HttpResponse = communityApiResponse("offender-female.json")

fun restrictiveRequirementsResponse(): HttpResponse = communityApiResponse("requirements-restrictive.json")

fun restrictiveAndNonRestrictiveRequirementsResponse(): HttpResponse = communityApiResponse("requirements-restrictive-and-non-restrictive.json")

fun nonRestrictiveRequirementsResponse(): HttpResponse = communityApiResponse("requirements-non-restrictive.json")

fun unpaidWorkRequirementsResponse(): HttpResponse = communityApiResponse("requirements-unpaid-work.json")

fun unpaidWorkWithOrderLengthExtendedAndAdditionalHoursRequirementsResponse(): HttpResponse = communityApiResponse("requirements-unpaid-work-additional-hours-order-length-extended.json")

fun noRequirementsResponse(): HttpResponse = jsonResponseOf(
  "{\n" +
    "    \"requirements\": []\n" +
    "}"
)

fun additionalRequirementsResponse(): HttpResponse = communityApiResponse("requirements-additional.json")

fun custodialAndNonCustodialConvictions(): HttpResponse = communityApiResponse("convictions-custodial-and-non-custodial.json")

fun nonCustodialCurrentAndTerminatedConviction(): HttpResponse = communityApiResponse("convictions-non-custodial-current-and-terminated.json")

fun assessmentsApiAssessmentsResponse(year: Int): HttpResponse = jsonResponseOf(
  responseFrom("$assessmentApiPath/assessments.json")
    .replace("completedDate", "$year-01-01T00:00:00")
    .replace("voidedDate", "")
)

fun assessmentsApiNoSeverityNeedsResponse(): HttpResponse =
  assessmentApiResponse("no_severity_needs.json")

fun assessmentsApi8NeedsResponse(): HttpResponse =
  assessmentApiResponse("8_points_needs.json")

fun assessmentsApiHighSeverityNeedsResponse(): HttpResponse =
  assessmentApiResponse("high_severity_needs_18_points.json")

private fun responseFrom(path: String) =
  readString(Paths.get(path))

private fun jsonResponseOf(response: String): HttpResponse =
  response().withContentType(APPLICATION_JSON).withBody(response)

private fun jsonResponseFromPath(path: String): HttpResponse =
  jsonResponseOf(responseFrom(path))

private fun communityApiResponse(path: String): HttpResponse =
  jsonResponseFromPath("$communityApiPath/$path")

private fun assessmentApiResponse(path: String): HttpResponse =
  jsonResponseFromPath("$assessmentApiPath/$path")
