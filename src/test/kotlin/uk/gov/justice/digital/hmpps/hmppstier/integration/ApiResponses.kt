package uk.gov.justice.digital.hmpps.hmppstier.integration

import java.nio.file.Files.readString
import java.nio.file.Paths

object ApiResponses {

  fun communityApiAssessmentsResponse(): String =
    responseFrom("src/test/resources/fixtures/community-api/assessments.json")

  fun emptyCommunityApiAssessmentsResponse(): String = "{}"

  fun emptyNsiResponse(): String = "{\"nsis\": []}"

  fun registrationsResponse(): String =
    responseFrom("src/test/resources/fixtures/community-api/registrations.json")

  fun emptyRegistrationsResponse(): String = "{}"

  fun registrationsResponseWithNoLevel(): String =
    responseFrom("src/test/resources/fixtures/community-api/registrations-no-level.json")

  fun custodialSCConvictionResponse(): String =
    responseFrom("src/test/resources/fixtures/community-api/convictions-custodial-sc.json")

  fun custodialNCConvictionResponse(): String =
    responseFrom("src/test/resources/fixtures/community-api/convictions-custodial-nc.json")

  fun custodialTerminatedConvictionResponse(): String =
    responseFrom("src/test/resources/fixtures/community-api/convictions-custodial-terminated.json")

  fun nonCustodialUnpaidWorkConvictionResponse(): String =
    responseFrom("src/test/resources/fixtures/community-api/convictions-non-custodial-unpaid-work.json")

  fun nonCustodialConvictionResponse(): String =
    responseFrom("src/test/resources/fixtures/community-api/convictions-non-custodial.json")

  fun noSentenceConvictionResponse(): String =
    responseFrom("src/test/resources/fixtures/community-api/convictions-no-sentence.json")

  fun nonCustodialTerminatedConvictionResponse(): String =
    responseFrom("src/test/resources/fixtures/community-api/convictions-non-custodial-terminated.json")

  fun maleOffenderResponse(): String =
    responseFrom("src/test/resources/fixtures/community-api/offender-male.json")

  fun femaleOffenderResponse(): String =
    responseFrom("src/test/resources/fixtures/community-api/offender-female.json")

  fun restrictiveRequirementsResponse(): String =
    responseFrom("src/test/resources/fixtures/community-api/requirements-restrictive.json")

  fun restrictiveAndNonRestrictiveRequirementsResponse(): String =
    responseFrom("src/test/resources/fixtures/community-api/requirements-restrictive-and-non-restrictive.json")

  fun nonRestrictiveRequirementsResponse(): String =
    responseFrom("src/test/resources/fixtures/community-api/requirements-non-restrictive.json")

  fun noRequirementsResponse(): String = "{\n" +
    "    \"requirements\": []\n" +
    "}"

  fun custodialAndNonCustodialUnpaid(): String =
    responseFrom("src/test/resources/fixtures/community-api/convictions-custodial-and-non-custodial-unpaid.json")

  fun nonCustodialCurrentAndTerminatedConvictionWithUnpaidWorkResponse(): String =
    responseFrom("src/test/resources/fixtures/community-api/convictions-non-custodial-current-and-terminated-with-unpaid-work.json")

  fun assessmentsApiAssessmentsResponse(year: Int): String =
    responseFrom("src/test/resources/fixtures/assessment-api/assessments.json")
      .replace("completedDate", "$year-01-01T00:00:00")
      .replace("voidedDate", "")

  fun assessmentsApiNoSeverityNeedsResponse(): String =
    responseFrom("src/test/resources/fixtures/assessment-api/no_severity_needs.json")

  fun assessmentsApiHighSeverityNeedsResponse(): String =
    responseFrom("src/test/resources/fixtures/assessment-api/high_severity_needs_18_points.json")

  private fun responseFrom(path: String) =
    readString(Paths.get(path))
}
