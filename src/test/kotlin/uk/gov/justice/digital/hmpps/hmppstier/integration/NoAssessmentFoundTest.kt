package uk.gov.justice.digital.hmpps.hmppstier.integration

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import uk.gov.justice.digital.hmpps.hmppstier.integration.ApiResponses.registrationsResponse

@TestInstance(PER_CLASS)
class NoAssessmentFoundTest : MockedEndpointsTestBase() {

  @Autowired
  lateinit var offenderEventsAmazonSQSAsync: AmazonSQSAsync

  @Value("\${offender-events.sqs-queue}")
  lateinit var eventQueueUrl: String

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
    expectTierCalculation("A2")
  }
}
