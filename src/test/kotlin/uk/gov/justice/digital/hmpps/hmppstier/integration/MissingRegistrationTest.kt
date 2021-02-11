package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel.ONE
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel.B
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.emptyRegistrationsResponse
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository
import uk.gov.justice.digital.hmpps.hmppstier.service.TierCalculationRequiredEventListener
import java.nio.file.Files
import java.nio.file.Paths

@TestInstance(PER_CLASS)
class MissingRegistrationTest : MockedEndpointsTestBase() {

  @Autowired
  lateinit var listener: TierCalculationRequiredEventListener

  @Autowired
  lateinit var repo: TierCalculationRepository

  @Test
  fun `calculate change and protect when no registrations are found`() {
    val crn = "X373878"
    setupNCCustodialSentence(crn)
    setupRegistrations(emptyRegistrationsResponse(), crn)
    restOfSetup(crn)
    val validMessage: String =
      Files.readString(Paths.get("src/test/resources/fixtures/sqs/tier-calculation-event.json"))
    listener.listen(validMessage)
    val tier = repo.findFirstByCrnOrderByCreatedDesc(crn)

    assertThat(tier?.data?.change?.tier).isEqualTo(ONE)
    assertThat(tier?.data?.protect?.tier).isEqualTo(B)
  }
}
