package uk.gov.justice.digital.hmpps.hmppstier.integration

import java.nio.file.Files
import java.nio.file.Paths

object ApiResponses {

  fun communityApiAssessmentsResponse(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/assessments.json"))
  fun registrationsResponse(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/registrations.json"))
  fun emptyRegistrationsResponse(): String = "{}"
  fun custodialSCConvictionResponse(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/convictions-custodial-sc.json"))
  fun custodialNCConvictionResponse(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/convictions-custodial-nc.json"))
  fun custodialTerminatedConvictionResponse(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/convictions-custodial-terminated.json"))
  fun nonCustodialUnpaidWorkConvictionResponse(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/convictions-non-custodial-unpaid-work.json"))
  fun nonCustodialConvictionResponse(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/convictions-non-custodial.json"))
  fun nonCustodialTerminatedConvictionResponse(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/convictions-non-custodial-terminated.json"))
  fun offenderResponse(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/offender.json"))
  fun restrictiveRequirementsResponse(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/requirements-restrictive.json"))
  fun nonRestrictiveRequirementsResponse(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/requirements-non-restrictive.json"))
  fun custodialAndNonCustodialUnpaid(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/convictions-custodial-and-non-custodial-unpaid.json"))
  fun nonCustodialCurrentAndTerminatedConvictionWithUnpaidWorkResponse(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/convictions-non-custodial-current-and-terminated-with-unpaid-work.json"))
  fun assessmentsApiAssessmentsResponse(year: Int): String =
    Files.readString(Paths.get("src/test/resources/fixtures/assessment-api/assessments.json")).replace("completedDate", "$year-01-01T00:00:00")
  fun assessmentsApiNeedsResponse(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/assessment-api/needs.json"))
}
