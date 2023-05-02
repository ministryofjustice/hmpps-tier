package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.CommunityApiExtension
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.CommunityApiExtension.Companion.communityApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.assessmentsApiNoSeverityNeedsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.emptyRegistrationsResponse
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.historicRegistrationsResponseWithMappa
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithLatestNonMappa
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithMappa
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponseWithNoLevel

class RegistrationEdgeCasesTest : IntegrationTestBase() {

  @Test
  fun `calculate change and protect when no registrations are found`() {
    val crn = "X473878"
    setupTierToDeliusFull(crn)
    communityApi.getCustodialNCSentenceConviction(crn)
    communityApi.getEmptyRegistration(crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = "7234567890")
    calculateTierFor(crn)
    expectTierChangedById("B1")
  }

  @Test
  fun `calculate change and protect when registration level is missing`() {
    val crn = "X445509"
    setupTierToDeliusFull(crn)
    communityApi.getCustodialNCSentenceConviction(crn)
    setupRegistrations(registrationsResponseWithNoLevel(), crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = "6234567890")
    calculateTierFor(crn)
    expectTierChangedById("B1")
  }

  @Test
  fun `uses latest registration - two mappa registrations present`() {
    val crn = "X445599"
    setupTierToDeliusFull(crn)
    communityApi.getCustodialNCSentenceConviction(crn)
    setupRegistrations(registrationsResponseWithMappa(), crn)
    setupMaleOffender(crn)
    setupNeeds(assessmentsApiNoSeverityNeedsResponse(), "6234507890")
    setupNoDeliusAssessment(crn)
    calculateTierFor(crn)
    expectTierChangedById("A2")
  }

  @Test
  fun `exclude historic registrations from tier calculation`() {
    val crn = "X445599"
    setupTierToDeliusNoAssessment(crn)
    communityApi.getCustodialNCSentenceConviction(crn)
    setupRegistrations(historicRegistrationsResponseWithMappa(), crn)
    setupMaleOffender(crn)
    setupNeeds(assessmentsApiNoSeverityNeedsResponse(), "6234507890")
    setupNoDeliusAssessment(crn)
    calculateTierFor(crn)
    expectLatestTierCalculation("D2")
  }

  @Test
  fun `uses mappa registration when latest is non-mappa but has mappa registration level`() {
    val crn = "X445599"
    setupTierToDeliusFull(crn)
    communityApi.getCustodialNCSentenceConviction(crn)
    setupRegistrations(registrationsResponseWithLatestNonMappa(), crn)
    setupMaleOffender(crn)
    setupNeeds(assessmentsApiNoSeverityNeedsResponse(), "6234507890")
    setupNoDeliusAssessment(crn)
    calculateTierFor(crn)
    expectTierChangedById("B2")
  }
}
