package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.AssessmentApiExtension
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.AssessmentApiExtension.Companion.assessmentApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.CommunityApiExtension.Companion.communityApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.assessmentsApiHighSeverityNeedsResponse

class TierCalculationTest : IntegrationTestBase() {

  @Nested
  inner class FemaleOffender {
    @Test
    fun `no NSis returned`() {
      val crn = "X386786"
      assessmentApi.getNotFoundAssessment(crn)
      setupTierToDeliusNoAssessment(crn, gender = "Female")

      communityApi.getCustodialNCSentenceConviction(crn)
      communityApi.getEmptyRegistration(crn)

      restOfSetupWithFemaleOffender(crn, 2234567890)
      communityApi.getEmptyNsiResponse(crn)

      calculateTierFor(crn)
      expectTierChangedById("D2")
    }
  }

  @Nested
  inner class TierChangeWriteback {
    @Test
    fun `Does not write back when tier is unchanged`() {
      val crn = "X432769"
      setupTierToDeliusFull(crn)

      communityApi.getCustodialSCSentenceConviction(crn)
      communityApi.getMappaRegistration(crn, "M2")
      restOfSetupWithMaleOffenderNoSevereNeeds(crn, false, 4234568890)
      assessmentApi.getOutdatedAssessment(crn, 1234567890)

      calculateTierFor(crn)
      expectTierChangedById("A2")

      communityApi.getCustodialSCSentenceConviction(crn)
      communityApi.getMappaRegistration(crn, "M2")
      restOfSetupWithMaleOffenderNoSevereNeeds(crn, false, 4234568890, "A2")
      assessmentApi.getOutdatedAssessment(crn, 1234567890)

      calculateTierFor(crn)
      expectNoUpdatedTierCalculation()
    }

    @Test
    fun `Does not write back when calculation result differs but tier is unchanged`() {
      val crn = "X432779"
      setupTierToDeliusFull(crn)

      communityApi.getCustodialSCSentenceConviction(crn)
      setupMaleOffenderWithRegistrations(crn, false, 4234568890)
      assessmentApi.getOutdatedAssessment(crn, 1234567890)

      calculateTierFor(crn)
      expectTierChangedById("A2")

      communityApi.getCustodialSCSentenceConviction(crn)
      communityApi.getMappaRegistration(crn, "M2")
      communityApi.getAssessmentResponse(crn, ogrs = "0")
      communityApi.getMaleOffenderResponse(crn, "A2")
      assessmentApi.getHighSeverityNeeds(4234568899)
      assessmentApi.getCurrentAssessment(crn, 4234568899) // assessment not out of date

      calculateTierFor(crn)
      expectNoUpdatedTierCalculation()
    }

    @Test
    fun `writes back when change level is changed`() {
      val crn = "X432770"
      setupTierToDeliusFull(crn)

      communityApi.getCustodialSCSentenceConviction(crn)
      setupMaleOffenderWithRegistrations(crn, false, 4234568890)
      assessmentApi.getOutdatedAssessment(crn, 4234568890)

      calculateTierFor(crn)
      expectTierChangedById("A2")

      setupTierToDeliusFull(crn)
      communityApi.getCustodialSCSentenceConviction(crn)
      setupMaleOffenderWithRegistrations(crn, false, 4234568891)
      assessmentApi.getCurrentAssessment(crn, 4234568891) // assessment not out of date
      calculateTierFor(crn)
      expectTierChangedById("A1")
    }

    @Test
    fun `writes back when protect level is changed`() {
      val crn = "X432771"
      setupTierToDeliusFull(crn)

      communityApi.getCustodialSCSentenceConviction(crn)
      setupMaleOffenderWithRegistrations(crn, assessmentId = 4234568890)

      calculateTierFor(crn)
      expectTierChangedById("A1")

      setupTierToDeliusFull(crn)
      communityApi.getCustodialSCSentenceConviction(crn)
      communityApi.getMappaRegistration(crn, "M1")
      restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 4234568890)

      calculateTierFor(crn)
      expectTierChangedById("B1")
    }
  }

  @Test
  fun `returns latest tier calculation`() {
    val crn = "X432777"
    setupTierToDeliusFull(crn)

    communityApi.getCustodialSCSentenceConviction(crn)
    setupMaleOffenderWithRegistrations(crn, assessmentId = 4234568890)

    calculateTierFor(crn)
    expectLatestTierCalculation("A1")

    setupTierToDeliusFull(crn)
    communityApi.getCustodialSCSentenceConviction(crn)
    communityApi.getMappaRegistration(crn, "M1")
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 4234568890)

    calculateTierFor(crn)
    expectLatestTierCalculation("B1")
  }

  @Test
  fun `404 from latest tier calculation if there is no calculation`() {
    val crn = "XNOCALC"
    expectLatestTierCalculationNotFound(crn)
  }

  @Test
  fun `404 from specified tier calculation`() {
    val crn = "XNOCALC"
    expectTierCalculationNotFound(crn, "5118f557-211e-4457-b75b-6df1f996d308")
  }

  @Test
  fun `400 from named tier calculation if calculationId is not valid`() {
    val crn = "XNOCALC"
    expectTierCalculationBadRequest(crn, "made-up-calculation-id")
  }
}
