package uk.gov.justice.digital.hmpps.hmppstier.integration

import java.nio.file.Files
import java.nio.file.Paths

object ApiResponses {

  fun communityApiAssessmentsResponse(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/assessments.json"))
  fun registrationsResponse(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/registrations.json"))
  fun custodialConvictionResponse(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/convictions-custodial.json"))
  fun nonCustodialUnpaidWorkConvictionResponse(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/convictions-non-custodial-unpaid-work.json"))
  fun nonCustodialConvictionResponse(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/convictions-non-custodial.json"))
  fun offenderResponse(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/offender.json"))
  fun restrictiveRequirementsResponse(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/requirements-restrictive.json"))
  fun nonRestrictiveRequirementsResponse(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/requirements-non-restrictive.json"))
  fun custodialAndNonCustodialUnpaid(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/community-api/convictions-custodial-and-non-custodial-unpaid.json"))
  fun assessmentsApiAssessmentsResponse(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/assessment-api/assessments.json"))
  fun assessmentsApiNeedsResponse(): String =
    Files.readString(Paths.get("src/test/resources/fixtures/assessment-api/needs.json"))
}
