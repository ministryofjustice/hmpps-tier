package uk.gov.justice.digital.hmpps.hmppstier.service

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException

class TriggerCalculationServiceTest {

  @Test
  fun `Missing Queue exception thrown when offender queue is not configured`() {
    val hmppsQueueService = mockk<HmppsQueueService>()
    val objectMapper = ObjectMapper()
    val hmppsOffenderSqsClient = mockk<AmazonSQSAsync>()

    every { hmppsQueueService.findByQueueId("hmppsoffenderqueue") } returns null

    Assertions.assertThrows(MissingQueueException::class.java) {
      TriggerCalculationService(hmppsQueueService, objectMapper, hmppsOffenderSqsClient)
    }
  }
}