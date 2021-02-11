package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockserver.integration.ClientAndServer
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationService

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MissingRegistrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var service: TierCalculationService

  // TODO move into a base class
  lateinit var mockCommunityApiServer: ClientAndServer
  lateinit var mockAssessmentApiServer: ClientAndServer

  @BeforeAll
  fun setupMockServer() {
    mockCommunityApiServer = ClientAndServer.startClientAndServer(8081)
    mockAssessmentApiServer = ClientAndServer.startClientAndServer(8082)
  }

  @AfterEach
  fun reset() {
    mockCommunityApiServer.reset()
    mockAssessmentApiServer.reset()
  }

  @AfterAll
  fun tearDownServer() {
    mockCommunityApiServer.stop()
    mockAssessmentApiServer.stop()
  }

  @Test
  fun `calculate change and protect when no registrations are found`() {
//    setupSCCustodialSentence()
//    restOfSetup()

    val tier = service.calculateTierForCrn("123")
    Assertions.assertThat(tier.data.change.tier).isEqualTo(ChangeLevel.ONE)
    Assertions.assertThat(tier.data.protect.tier).isEqualTo(ProtectLevel.A)
  }

}