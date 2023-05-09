package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.assessmentApi.AssessmentApiExtension.Companion.assessmentApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.TierToDeliusApiExtension.Companion.tierToDeliusApi
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.Registration
import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.tierToDeliusApi.response.domain.TierDetails
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.IntegrationTestBase
import java.time.LocalDate

class RegistrationEdgeCasesTest : IntegrationTestBase() {

  @Test
  fun `calculate change and protect when no registrations are found`() {
    val crn = "X473878"
    tierToDeliusApi.getFullDetails(crn, TierDetails(convictions = listOf(Conviction())))
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 7234567890)
    calculateTierFor(crn)
    expectTierChangedById("B1")
  }

  @Test
  fun `calculate change and protect when registration level is missing`() {
    val crn = "X445509"
    tierToDeliusApi.getFullDetails(
      crn,
      TierDetails(
        convictions = listOf(Conviction()),
        registrations = listOf(Registration(typeCode = "STRG")),
      ),
    )
    restOfSetupWithMaleOffenderNoSevereNeeds(crn, assessmentId = 6234567890)
    calculateTierFor(crn)
    expectTierChangedById("B1")
  }

  @Test
  fun `uses latest registration - two mappa registrations present`() {
    val crn = "X445599"
    tierToDeliusApi.getFullDetails(
      crn,
      TierDetails(
        ogrsScore = null,
        rsrScore = null,
        convictions = listOf(Conviction()),
        registrations = listOf(
          Registration(registerLevel = "M1", startDate = LocalDate.of(2020, 2, 1)),
          Registration(registerLevel = "M2", startDate = LocalDate.of(2021, 2, 1)),
        ),
      ),
    )
    assessmentApi.getNoSeverityNeeds(6234507890)
    calculateTierFor(crn)
    expectTierChangedById("A2")
  }

  @Test
  fun `exclude historic registrations from tier calculation`() {
    val crn = "X445599"
    tierToDeliusApi.getFullDetails(
      crn,
      TierDetails(
        ogrsScore = null,
        rsrScore = null,
        convictions = listOf(Conviction()),
        registrations = listOf(
          Registration(registerLevel = "M2", typeCode = "HREG"),
        ),
      ),
    )
    assessmentApi.getNoSeverityNeeds(6234507890)
    calculateTierFor(crn)
    expectLatestTierCalculation("D2")
  }

  @Test
  fun `uses mappa registration when latest is non-mappa but has mappa registration level`() {
    val crn = "X445599"
    tierToDeliusApi.getFullDetails(
      crn,
      TierDetails(
        convictions = listOf(Conviction()),
        registrations = listOf(
          Registration(registerLevel = "M2", typeCode = "HREG", startDate = LocalDate.of(2016, 6, 28)),
          Registration(registerLevel = "M0", startDate = LocalDate.of(2008, 10, 24)),
        ),
      ),
    )
    assessmentApi.getNoSeverityNeeds(6234507890)
    calculateTierFor(crn)
    expectTierChangedById("B2")
  }
}
