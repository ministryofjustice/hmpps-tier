package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.AssessmentApiExtension
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.AssessmentApiExtension.Companion.assessmentApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.CommunityApiExtension.Companion.communityApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase

class CannotCalculateTierTest : IntegrationTestBase() {

  @Test
  fun `Offender does not exist`() {
    val crn = "X123456"
    assessmentApi.getCurrentAssessment(crn, 1234567890)
    communityApi.getCustodialSCSentenceConviction(crn)
    communityApi.getEmptyRegistration(crn)
    communityApi.getAssessmentResponse(crn)
    communityApi.getNotFoundOffenderResponse(crn)
    calculateTierFor(crn)
    expectTierCalculationToHaveFailed()
  }
}
