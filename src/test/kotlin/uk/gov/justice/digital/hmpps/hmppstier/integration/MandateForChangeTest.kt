package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppstier.controller.TierCalculationRequiredEventListener
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository

@TestInstance(PER_CLASS)
class MandateForChangeTest : MockedEndpointsTestBase() {

  @Autowired
  lateinit var listener: TierCalculationRequiredEventListener

  @Autowired
  lateinit var repo: TierCalculationRepository

  @Test
  fun `do not calculate change for a non-custodial sentence with only restrictive requirements`() {
    val crn = "X232323"

    setupNonCustodialSentenceWithNoUnpaidWork(crn)
    setupRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)

    listener.listen(calculationMessage(crn))
    val tier = repo.findFirstByCrnOrderByCreatedDesc(crn)

    Assertions.assertThat(tier?.data?.change?.tier).isEqualTo(ChangeLevel.ZERO)
    Assertions.assertThat(tier?.data?.protect?.tier).isEqualTo(ProtectLevel.A)
  }

  @Test
  fun `calculate change for concurrent custodial and non-custodial sentence`() {
    val crn = "X676767"

    setupConcurrentCustodialAndNonCustodialSentenceWithUnpaidWork(crn)
    setupRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)

    listener.listen(calculationMessage(crn))

    val tier = repo.findFirstByCrnOrderByCreatedDesc(crn)

    Assertions.assertThat(tier?.data?.change?.tier).isEqualTo(ChangeLevel.ONE)
    Assertions.assertThat(tier?.data?.protect?.tier).isEqualTo(ProtectLevel.A)
  }

  @Test
  fun `calculate change and protect for SC custodial sentence`() {
    val crn = "X373878"
    setupSCCustodialSentence(crn)
    setupMaleOffenderWithRegistrations(crn)

    listener.listen(calculationMessage(crn))

    val tier = repo.findFirstByCrnOrderByCreatedDesc(crn)

    Assertions.assertThat(tier?.data?.change?.tier).isEqualTo(ChangeLevel.ONE)
    Assertions.assertThat(tier?.data?.protect?.tier).isEqualTo(ProtectLevel.A)
  }

  @Test
  fun `calculate change and protect for NC custodial sentence`() {
    val crn = "123"
    setupNCCustodialSentence(crn)
    setupMaleOffenderWithRegistrations(crn)

    listener.listen(calculationMessage(crn))

    val tier = repo.findFirstByCrnOrderByCreatedDesc(crn)

    Assertions.assertThat(tier?.data?.change?.tier).isEqualTo(ChangeLevel.ONE)
    Assertions.assertThat(tier?.data?.protect?.tier).isEqualTo(ProtectLevel.A)
  }

  @Test
  fun `do not calculate change for terminated custodial sentence`() {
    val crn = "X373878"
    setupTerminatedCustodialSentence(crn)
    setupMaleOffenderWithRegistrations(crn)

    listener.listen(calculationMessage(crn))

    val tier = repo.findFirstByCrnOrderByCreatedDesc(crn)

    Assertions.assertThat(tier?.data?.change?.tier).isEqualTo(ChangeLevel.ZERO)
    Assertions.assertThat(tier?.data?.protect?.tier).isEqualTo(ProtectLevel.A)
  }

  @Test
  fun `calculate change for terminated non-custodial sentence and current non-custodial sentence with non-restrictive requirements`() {
    val crn = "X505050"
    setupCurrentNonCustodialSentenceAndTerminatedNonCustodialSentenceWithUnpaidWork(crn)
    setupNonRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)

    listener.listen(calculationMessage(crn))

    val tier = repo.findFirstByCrnOrderByCreatedDesc(crn)

    Assertions.assertThat(tier?.data?.change?.tier).isEqualTo(ChangeLevel.ONE)
    Assertions.assertThat(tier?.data?.protect?.tier).isEqualTo(ProtectLevel.A)
  }

  @Test
  fun `calculate change and protect for non-custodial sentence with non-restrictive requirements`() {
    val crn = "X222222"

    setupNonCustodialSentenceWithNoUnpaidWork(crn)
    setupNonRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)

    listener.listen(calculationMessage(crn))

    val tier = repo.findFirstByCrnOrderByCreatedDesc(crn)

    Assertions.assertThat(tier?.data?.change?.tier).isEqualTo(ChangeLevel.ONE)
    Assertions.assertThat(tier?.data?.protect?.tier).isEqualTo(ProtectLevel.A)
  }

  @Test
  fun `do not calculate change for terminated non-custodial sentence with non-restrictive requirements`() {
    val crn = "X888888"

    setupTerminatedNonCustodialSentenceWithNoUnpaidWork(crn)
    setupNonRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)

    listener.listen(calculationMessage(crn))

    val tier = repo.findFirstByCrnOrderByCreatedDesc(crn)

    Assertions.assertThat(tier?.data?.change?.tier).isEqualTo(ChangeLevel.ZERO)
    Assertions.assertThat(tier?.data?.protect?.tier).isEqualTo(ProtectLevel.A)
  }

  @Test
  fun `do not calculate change when only restrictive requirements are present on a non-custodial sentence`() {
    val crn = "X888866"

    setupNonCustodialSentenceWithNoUnpaidWork(crn)
    setupRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)

    listener.listen(calculationMessage(crn))

    val tier = repo.findFirstByCrnOrderByCreatedDesc(crn)

    Assertions.assertThat(tier?.data?.change?.tier).isEqualTo(ChangeLevel.ZERO)
    Assertions.assertThat(tier?.data?.protect?.tier).isEqualTo(ProtectLevel.A)
  }

  @Test
  fun `calculate change with restrictive and non-restrictive requirements on a non-custodial sentence`() {
    val crn = "X888855"

    setupNonCustodialSentenceWithNoUnpaidWork(crn)
    setupRestrictiveAndNonRestrictiveRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)

    listener.listen(calculationMessage(crn))

    val tier = repo.findFirstByCrnOrderByCreatedDesc(crn)

    Assertions.assertThat(tier?.data?.change?.tier).isEqualTo(ChangeLevel.ONE)
    Assertions.assertThat(tier?.data?.protect?.tier).isEqualTo(ProtectLevel.A)
  }

  @Test
  fun `do not calculate change when no requirements are present on a non-custodial sentence`() {
    val crn = "X888844"

    setupNonCustodialSentenceWithUnpaidWork(crn)
    setupNoRequirements(crn)
    setupMaleOffenderWithRegistrations(crn)

    listener.listen(calculationMessage(crn))

    val tier = repo.findFirstByCrnOrderByCreatedDesc(crn)

    Assertions.assertThat(tier?.data?.change?.tier).isEqualTo(ChangeLevel.ZERO)
    Assertions.assertThat(tier?.data?.protect?.tier).isEqualTo(ProtectLevel.A)
  }
}
