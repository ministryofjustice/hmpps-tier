package uk.gov.justice.digital.hmpps.hmppstier.integration.setup

import org.mockserver.model.HttpResponse
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType.APPLICATION_JSON
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.IMPULSIVITY
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.PARENTING_RESPONSIBILITIES
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AdditionalFactorForWomen.TEMPER_CONTROL
import java.nio.file.Files.readString
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_DATE
import java.time.format.DateTimeFormatter.ISO_DATE_TIME

const val COMMUNITY_API_FIXTURES: String = "src/test/resources/fixtures/community-api"
const val ASSESSMENT_API_FIXTURES: String = "src/test/resources/fixtures/assessment-api"
const val TIERTODELIUS_API_FIXTURES: String = "src/test/resources/fixtures/tierToDelius-api"

fun communityApiAssessmentsResponse(rsr: String, ogrs: String): HttpResponse = jsonResponseOf(
  responseFrom("$COMMUNITY_API_FIXTURES/assessments.json")
    .replace("rsrScoreToReplace", rsr)
    .replace("ogrsScoreToReplace", ogrs),
)

fun emptyNsisResponse(): HttpResponse = jsonResponseOf("{\"nsis\": []}")

fun nsisResponse(outcome: String): HttpResponse = jsonResponseOf(
  responseFrom("$COMMUNITY_API_FIXTURES/nsi-breach.json")
    .replace("nsiOutcomeToReplace", outcome),
)


fun registrationsResponseWithRoshMappaAndAdditionalFactors(rosh: String, mappa: String, factors: List<String>) =
  registrations(
    responseFrom("$COMMUNITY_API_FIXTURES/registrations-rosh.json")
      .replace("roshToReplace", rosh) + "," + responseFrom("$COMMUNITY_API_FIXTURES/registrations-mappa.json")
      .replace("mappaToReplace", mappa) + "," + additionalFactors(factors),
  )

fun registrationsResponseWithMappaAndAdditionalFactors(mappa: String, factors: List<String>) =
  registrations(
    responseFrom("$COMMUNITY_API_FIXTURES/registrations-mappa.json")
      .replace("mappaToReplace", mappa) + "," + additionalFactors(factors),
  )

fun registrationsResponseWithAdditionalFactors(additionalFactors: List<String>): HttpResponse {
  val factors: String = additionalFactors(additionalFactors)
  return registrations(factors)
}

fun needsResponse(needs: Map<String, String>): HttpResponse {
  val needsResponse: List<String> = needs.map {
    responseFrom("$ASSESSMENT_API_FIXTURES/needs-additional.json")
      .replace("needToReplace", it.key)
      .replace("severityToReplace", it.value)
  }
  return jsonResponseOf(
    "[" +
      needsResponse.toTypedArray().joinToString(separator = ",") +
      "]",
  )
}

private fun additionalFactors(additionalFactors: List<String>): String = additionalFactors.map {
  responseFrom("$COMMUNITY_API_FIXTURES/registrations-additional.json")
    .replace("additionalFactorsToReplace", it)
}.toTypedArray().joinToString(separator = ",")

private fun registrations(registrations: String) = jsonResponseOf(
  "{\"registrations\": [" +
    registrations +
    "]}",
)

fun maleOffenderResponse(tier: String = "A1"): HttpResponse = jsonResponseOf(
  responseFrom("$COMMUNITY_API_FIXTURES/offender-male.json").replace("tierToReplace", tier.replaceFirstChar { it.plus("_") }),
)

fun femaleOffenderResponse(): HttpResponse = communityApiResponse("offender-female.json")

fun restrictiveRequirementsResponse(): HttpResponse = communityApiResponse("requirements-restrictive.json")

fun restrictiveAndNonRestrictiveRequirementsResponse(): HttpResponse =
  communityApiResponse("requirements-restrictive-and-non-restrictive.json")

fun nonRestrictiveRequirementsResponse(): HttpResponse = communityApiResponse("requirements-non-restrictive.json")

fun unpaidWorkRequirementsResponse(): HttpResponse = communityApiResponse("requirements-unpaid-work.json")

fun unpaidWorkWithOrderLengthExtendedAndAdditionalHoursRequirementsResponse(): HttpResponse =
  communityApiResponse("requirements-unpaid-work-additional-hours-order-length-extended.json")

fun noRequirementsResponse(): HttpResponse = jsonResponseOf(
  "{\n" +
    "    \"requirements\": []\n" +
    "}",
)

fun additionalRequirementsResponse(): HttpResponse = communityApiResponse("requirements-additional.json")

fun assessmentsApiAssessmentsResponse(assessmentDate: LocalDateTime, assessmentId: String): HttpResponse =
  jsonResponseOf(
    responseFrom("$ASSESSMENT_API_FIXTURES/assessments.json")
      .replace("completedDate", assessmentDate.format(ISO_DATE_TIME))
      .replace("voidedDate", "")
      .replace("\"assessmentIdToReplace\"", assessmentId),
  )

fun assessmentsApiNoSeverityNeedsResponse(): HttpResponse =
  assessmentApiResponse("no_severity_needs.json")

fun tierToDeliusFullResponse(
  gender: String = "Male",
  currentTier: String = "UD0",
  ogrsscore: String,
  rsrscore: String,
): HttpResponse =
  jsonResponseOf(
    responseFrom("$TIERTODELIUS_API_FIXTURES/tier-to-delius-response.json")
      .replace("genderToReplace", gender)
      .replace("tierToReplace", currentTier)
      .replace("\"ogrsToReplace\"", ogrsscore)
      .replace("\"rsrToReplace\"", rsrscore),
  )

fun tierToDeliusNoAssessmentResponse(gender: String, currentTier: String): HttpResponse =
  jsonResponseOf(
    responseFrom("$TIERTODELIUS_API_FIXTURES/tier-to-delius-no-assessment-response.json")
      .replace("genderToReplace", gender)
      .replace("tierToReplace", currentTier),
  )

fun assessmentsApiHighSeverityNeedsResponse(): HttpResponse =
  assessmentApiResponse("high_severity_needs_18_points.json")

fun assessmentsApiFemaleAnswersResponse(assessmentAnswers: Map<String, String>, assessmentId: String): HttpResponse =
  jsonResponseOf(
    responseFrom("$ASSESSMENT_API_FIXTURES/female-answers.json")
      .replace("6.9AnswerToReplace", assessmentAnswers[PARENTING_RESPONSIBILITIES.answerCode]!!)
      .replace("11.2AnswerToReplace", assessmentAnswers[IMPULSIVITY.answerCode]!!)
      .replace("11.4AnswerToReplace", assessmentAnswers[TEMPER_CONTROL.answerCode]!!)
      .replace("\"assessmentIdToReplace\"", assessmentId),

  )

private fun responseFrom(path: String) =
  readString(Paths.get(path))

private fun jsonResponseOf(response: String): HttpResponse =
  response().withContentType(APPLICATION_JSON).withBody(response)

private fun jsonResponseFromPath(path: String): HttpResponse =
  jsonResponseOf(responseFrom(path))

private fun communityApiResponse(path: String): HttpResponse =
  jsonResponseFromPath("$COMMUNITY_API_FIXTURES/$path")

private fun assessmentApiResponse(path: String): HttpResponse =
  jsonResponseFromPath("$ASSESSMENT_API_FIXTURES/$path")
