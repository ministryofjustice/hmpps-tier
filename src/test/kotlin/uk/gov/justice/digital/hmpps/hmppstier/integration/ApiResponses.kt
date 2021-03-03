package uk.gov.justice.digital.hmpps.hmppstier.integration

import java.nio.file.Files.readString
import java.nio.file.Paths

object ApiResponses {

  fun communityApiAssessmentsResponse(): String =
    readString(Paths.get("src/test/resources/fixtures/community-api/assessments.json"))

  fun registrationsResponse(): String =
    readString(Paths.get("src/test/resources/fixtures/community-api/registrations.json"))

  fun emptyRegistrationsResponse(): String = "{}"
  fun registrationsResponseWithNoLevel(): String = readString(Paths.get("src/test/resources/fixtures/community-api/registrations-no-level.json"))
  fun custodialSCConvictionResponse(): String =
    readString(Paths.get("src/test/resources/fixtures/community-api/convictions-custodial-sc.json"))

  fun custodialNCConvictionResponse(): String =
    readString(Paths.get("src/test/resources/fixtures/community-api/convictions-custodial-nc.json"))

  fun custodialTerminatedConvictionResponse(): String =
    readString(Paths.get("src/test/resources/fixtures/community-api/convictions-custodial-terminated.json"))

  fun nonCustodialUnpaidWorkConvictionResponse(): String =
    readString(Paths.get("src/test/resources/fixtures/community-api/convictions-non-custodial-unpaid-work.json"))

  fun nonCustodialConvictionResponse(): String =
    readString(Paths.get("src/test/resources/fixtures/community-api/convictions-non-custodial.json"))

  fun nonCustodialTerminatedConvictionResponse(): String =
    readString(Paths.get("src/test/resources/fixtures/community-api/convictions-non-custodial-terminated.json"))

  fun maleOffenderResponse(): String =
    readString(Paths.get("src/test/resources/fixtures/community-api/offender-male.json"))

  fun restrictiveRequirementsResponse(): String =
    readString(Paths.get("src/test/resources/fixtures/community-api/requirements-restrictive.json"))

  fun nonRestrictiveRequirementsResponse(): String =
    readString(Paths.get("src/test/resources/fixtures/community-api/requirements-non-restrictive.json"))

  fun custodialAndNonCustodialUnpaid(): String =
    readString(Paths.get("src/test/resources/fixtures/community-api/convictions-custodial-and-non-custodial-unpaid.json"))

  fun nonCustodialCurrentAndTerminatedConvictionWithUnpaidWorkResponse(): String =
    readString(Paths.get("src/test/resources/fixtures/community-api/convictions-non-custodial-current-and-terminated-with-unpaid-work.json"))

  fun assessmentsApiAssessmentsResponse(year: Int): String =
    readString(Paths.get("src/test/resources/fixtures/assessment-api/assessments.json"))
      .replace("completedDate", "$year-01-01T00:00:00")
      .replace("voidedDate", "")

  fun assessmentsApiNeedsResponse(): String =
    readString(Paths.get("src/test/resources/fixtures/assessment-api/needs.json"))
}
