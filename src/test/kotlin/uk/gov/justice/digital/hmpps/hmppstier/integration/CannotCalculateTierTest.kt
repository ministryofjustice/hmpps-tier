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
    setupTierToDeliusNotFound(crn)
    calculateTierFor(crn)
    expectTierCalculationToHaveFailed()
  }
}
