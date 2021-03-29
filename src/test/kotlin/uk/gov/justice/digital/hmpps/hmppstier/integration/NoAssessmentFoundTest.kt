package uk.gov.justice.digital.hmpps.hmppstier.integration

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel.TWO
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel.A
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.registrationsResponse
import uk.gov.justice.digital.hmpps.hmppstier.jpa.repository.TierCalculationRepository

@TestInstance(PER_CLASS)
class NoAssessmentFoundTest : MockedEndpointsTestBase() {

  @Autowired
  lateinit var offenderEventsAmazonSQSAsync: AmazonSQSAsync

  @Autowired
  lateinit var calculationCompleteClient: AmazonSQSAsync

  @Value("\${offender-events.sqs-queue}")
  lateinit var eventQueueUrl: String

  @Value("\${calculation-complete.sqs-queue}")
  lateinit var calculationCompleteUrl: String

  @Autowired
  lateinit var repo: TierCalculationRepository

  @BeforeEach
  fun `purge Queues`() {
    offenderEventsAmazonSQSAsync.purgeQueue(PurgeQueueRequest(eventQueueUrl))
    calculationCompleteClient.purgeQueue(PurgeQueueRequest(calculationCompleteUrl))
  }

  @Test
  fun `changeLevel should be 2 if assessment returns 404`() {
    val crn = "X373878"
    setupNCCustodialSentence(crn)
    setupAssessmentNotFound(crn)
    setupRegistrations(registrationsResponse(), crn)
    restOfSetupWithMaleOffenderNoSevereNeeds(crn)
    putMessageOnQueue(offenderEventsAmazonSQSAsync, eventQueueUrl, crn)
    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue(calculationCompleteClient, calculationCompleteUrl) } matches { it == 1 }

    val tier = repo.findFirstByCrnOrderByCreatedDesc(crn)
    assertThat(tier?.data?.change?.tier).isEqualTo(TWO)
    assertThat(tier?.data?.protect?.tier).isEqualTo(A)
  }
}
